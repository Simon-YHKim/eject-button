import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Measures how much of a release build R8 actually renamed.
 *
 * Why this exists
 * ---------------
 * The Play Console "app optimization" panel shows an obfuscation percentage and
 * warns below 25%: "Google Play에서 앱의 공개 상태와 게시 기능에 영향을 미칠 수
 * 있습니다". For 1091 (1.7.4) it read 23%, because proguard-rules.pro carried
 * blanket "-keep class androidx.compose.** { *; }" style rules that exempted
 * 63.57% of all classes from renaming. See the v1.7.6 entry in CHANGELOG.md.
 *
 * No build is needed to audit a shipped bundle: an AAB embeds the whole R8 map
 * at BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map. A plain
 * app/build/outputs/mapping/release/mapping.txt works too.
 *
 * Play does not publish its formula. Our class-level figure came out 20.23%
 * against the Play reading of 23% for the same bundle, so this is a proxy that
 * tracks the same thing rather than a reproduction of the Play metric. It is
 * accurate enough to separate "well under the floor" from "well over it".
 *
 * Usage
 *   java scripts/MeasureObfuscation.java --self-test
 *   java scripts/MeasureObfuscation.java app/build/outputs/mapping/release/mapping.txt
 *   java scripts/MeasureObfuscation.java --minimum-class-ratio 25 some-release.aab
 *   java scripts/MeasureObfuscation.java --top 15 some-release.aab
 *
 * Exit codes: 0 ok, 1 below --minimum-class-ratio, 2 bad usage or unreadable input.
 */
public final class MeasureObfuscation {

    private static final String MAP_ENTRY =
            "BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map";

    public static void main(String[] args) {
        List<String> inputs = new ArrayList<>();
        double minimumClassRatio = -1;
        int top = 10;
        boolean selfTest = false;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--self-test")) {
                selfTest = true;
            } else if (a.equals("--minimum-class-ratio")) {
                minimumClassRatio = Double.parseDouble(requireValue(args, ++i, a));
            } else if (a.equals("--top")) {
                top = Integer.parseInt(requireValue(args, ++i, a));
            } else if (a.equals("--help") || a.equals("-h")) {
                usage();
                return;
            } else if (a.startsWith("--")) {
                fail("unknown flag: " + a);
            } else {
                inputs.add(a);
            }
        }

        if (selfTest) {
            selfTest();
            if (inputs.isEmpty()) {
                return;
            }
        }
        if (inputs.isEmpty()) {
            usage();
            System.exit(2);
        }

        int exit = 0;
        for (String input : inputs) {
            Stats s = measure(Path.of(input));
            s.print(input, top);
            if (minimumClassRatio >= 0) {
                double got = s.classRatio();
                if (got < minimumClassRatio) {
                    System.out.printf(Locale.ROOT,
                            "FAIL: class obfuscation %.2f%% is below the required %.2f%%%n",
                            got, minimumClassRatio);
                    exit = 1;
                } else {
                    System.out.printf(Locale.ROOT,
                            "OK: class obfuscation %.2f%% meets the required %.2f%%%n",
                            got, minimumClassRatio);
                }
            }
            System.out.println();
        }
        System.exit(exit);
    }

    private static String requireValue(String[] args, int i, String flag) {
        if (i >= args.length) {
            fail(flag + " needs a value");
        }
        return args[i];
    }

    private static void fail(String message) {
        System.err.println("error: " + message);
        System.exit(2);
    }

    private static void usage() {
        System.out.println("usage: java scripts/MeasureObfuscation.java [--self-test] "
                + "[--minimum-class-ratio PCT] [--top N] <mapping.txt | app.aab> ...");
    }

    /** Counts renamed vs total entries in an R8 mapping file. */
    static final class Stats {
        long classTotal;
        long classRenamed;
        long methodTotal;
        long methodRenamed;
        long fieldTotal;
        long fieldRenamed;
        final Map<String, Long> keptByPackage = new HashMap<>();

        double classRatio() {
            return classTotal == 0 ? 0 : 100.0 * classRenamed / classTotal;
        }

        private static double ratio(long renamed, long total) {
            return total == 0 ? 0 : 100.0 * renamed / total;
        }

        void print(String label, int top) {
            System.out.println("### " + label);
            System.out.printf(Locale.ROOT, "  classes : total %,9d  renamed %,9d  = %6.2f%%%n",
                    classTotal, classRenamed, ratio(classRenamed, classTotal));
            System.out.printf(Locale.ROOT, "  methods : total %,9d  renamed %,9d  = %6.2f%%%n",
                    methodTotal, methodRenamed, ratio(methodRenamed, methodTotal));
            System.out.printf(Locale.ROOT, "  fields  : total %,9d  renamed %,9d  = %6.2f%%%n",
                    fieldTotal, fieldRenamed, ratio(fieldRenamed, fieldTotal));
            if (top <= 0 || keptByPackage.isEmpty()) {
                return;
            }
            System.out.println("  kept (not renamed) classes by package prefix:");
            keptByPackage.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(top)
                    .forEach(e -> System.out.printf(Locale.ROOT, "      %,7d  %s%n",
                            e.getValue(), e.getKey()));
        }
    }

    static Stats measure(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".aab") || name.endsWith(".zip")) {
                try (ZipFile zip = new ZipFile(path.toFile())) {
                    ZipEntry entry = zip.getEntry(MAP_ENTRY);
                    if (entry == null) {
                        System.err.println("error: " + path + " has no " + MAP_ENTRY
                                + " - was it built with minification enabled?");
                        System.exit(2);
                    }
                    try (InputStream in = zip.getInputStream(entry)) {
                        return measure(new BufferedReader(
                                new InputStreamReader(in, StandardCharsets.UTF_8)));
                    }
                }
            }
            try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                return measure(r);
            }
        } catch (IOException e) {
            System.err.println("error: cannot read " + path + ": " + e.getMessage());
            System.exit(2);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The R8 map grammar we care about:
     *
     *   com.foo.Bar -> a.b.c:                 class line, no leading space, ends with a colon
     *       java.lang.String field -> a       field line, indented, no parenthesis
     *       1:10:void run():12:21 -> b        method line, indented, has parentheses
     *
     * R8 emits one method line per inlined frame, so a single source method can
     * appear many times inside one class. We de-duplicate on name plus parameter
     * list per class; otherwise heavily-inlined code inflates the method totals.
     */
    static Stats measure(BufferedReader reader) throws IOException {
        Stats s = new Stats();
        Set<String> seenMembers = new HashSet<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.stripLeading().startsWith("#")) {
                continue;
            }
            char c0 = line.charAt(0);
            boolean indented = c0 == ' ' || c0 == '\t';
            int arrow = line.lastIndexOf(" -> ");
            if (arrow < 0) {
                continue;
            }
            String left = line.substring(0, arrow).strip();
            String right = line.substring(arrow + 4).strip();

            if (!indented) {
                if (!right.endsWith(":")) {
                    continue;
                }
                String renamed = right.substring(0, right.length() - 1);
                s.classTotal++;
                if (renamed.equals(left)) {
                    s.keptByPackage.merge(packagePrefix(left), 1L, Long::sum);
                } else {
                    s.classRenamed++;
                }
                seenMembers.clear();
                continue;
            }

            int paren = left.indexOf('(');
            if (paren >= 0) {
                String original = stripLineNumbers(lastToken(left.substring(0, paren)));
                int close = left.indexOf(')');
                String params = close < 0 ? left.substring(paren)
                        : left.substring(paren, close + 1);
                if (!seenMembers.add(original + params)) {
                    continue;
                }
                s.methodTotal++;
                if (!right.equals(original)) {
                    s.methodRenamed++;
                }
            } else {
                String original = lastToken(left);
                s.fieldTotal++;
                if (!right.equals(original)) {
                    s.fieldRenamed++;
                }
            }
        }
        return s;
    }

    private static String lastToken(String s) {
        int sp = s.lastIndexOf(' ');
        return sp < 0 ? s : s.substring(sp + 1);
    }

    /** Turns a "1:10:void" style prefix into just the trailing identifier. */
    private static String stripLineNumbers(String s) {
        int colon = s.lastIndexOf(':');
        return colon < 0 ? s : s.substring(colon + 1);
    }

    /** First two dot-segments, so androidx.compose.foo.Bar groups under androidx.compose. */
    static String packagePrefix(String className) {
        int first = className.indexOf('.');
        if (first < 0) {
            return className;
        }
        int second = className.indexOf('.', first + 1);
        return second < 0 ? className : className.substring(0, second);
    }

    private static void selfTest() {
        String map = String.join("\n",
                "# compiler: R8",
                "com.ejectbutton.MainActivity -> com.ejectbutton.MainActivity:",
                "    int counter -> counter",
                "    1:5:void onCreate(android.os.Bundle):10:14 -> onCreate",
                "    6:9:void onCreate(android.os.Bundle):20:23 -> onCreate",
                "com.ejectbutton.ui.Widget -> t2.a:",
                "    java.lang.String label -> b",
                "    void render() -> c",
                "androidx.compose.ui.Modifier -> androidx.compose.ui.Modifier:",
                "    void apply() -> apply",
                "");

        Stats s;
        try {
            s = measure(new BufferedReader(new StringReader(map)));
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        check("classTotal", 3, s.classTotal);
        check("classRenamed", 1, s.classRenamed);
        check("methodTotal", 3, s.methodTotal);
        check("methodRenamed", 1, s.methodRenamed);
        check("fieldTotal", 2, s.fieldTotal);
        check("fieldRenamed", 1, s.fieldRenamed);
        check("kept androidx.compose", 1, s.keptByPackage.getOrDefault("androidx.compose", 0L));
        check("kept com.ejectbutton", 1, s.keptByPackage.getOrDefault("com.ejectbutton", 0L));

        if (!"androidx.compose".equals(packagePrefix("androidx.compose.ui.Modifier"))) {
            throw new AssertionError("self-test packagePrefix");
        }
        if (!"Foo".equals(packagePrefix("Foo"))) {
            throw new AssertionError("self-test packagePrefix on default package");
        }

        System.out.println("self-test: OK (10 assertions)");
    }

    private static void check(String what, long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("self-test " + what + ": expected " + expected
                    + " but got " + actual);
        }
    }

    private MeasureObfuscation() {
    }
}
