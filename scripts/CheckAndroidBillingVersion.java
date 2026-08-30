import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.concurrent.TimeUnit;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Verifies Play Billing metadata from built APK/AAB files.
 *
 * Run directly with JDK 17 source-file mode:
 *   java scripts/CheckAndroidBillingVersion.java --self-test
 *   java scripts/CheckAndroidBillingVersion.java --expected-billing 9.1.0 artifact.apk artifact.aab
 */
public final class CheckAndroidBillingVersion {
    private static final String DEFAULT_MINIMUM_BILLING = "8.0.0";
    private static final int MAX_BILLING_PROPERTIES_BYTES = 1024 * 1024;
    private static final int MAX_MANIFEST_BYTES = 32 * 1024 * 1024;
    private static final int MAX_BUNDLETOOL_ERROR_BYTES = 1024 * 1024;
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final Pattern BILLING_KEY = Pattern.compile(
        "(?m)^\\s*billing_client\\s*="
    );
    private static final Pattern BILLING_VERSION = Pattern.compile(
        "(?m)^\\s*billing_client\\s*=\\s*([0-9]+\\.[0-9]+\\.[0-9]+)\\s*$"
    );

    private static final int RES_VERSION_CODE = 0x0101021b;
    private static final int RES_VERSION_NAME = 0x0101021c;

    private record Options(
        String minimumBilling,
        String expectedBilling,
        Integer minimumVersionCode,
        Integer expectedVersionCode,
        String expectedVersionName,
        String expectedPackage,
        Path bundletool,
        List<Path> artifacts
    ) {}

    private record ManifestMetadata(
        int versionCode,
        String versionName,
        String applicationId
    ) {}

    private record ArtifactResult(
        Path path,
        String type,
        String billingEntry,
        String billingVersion,
        ManifestMetadata manifest
    ) {}

    public static void main(String[] args) {
        try {
            if (args.length == 1 && "--self-test".equals(args[0])) {
                runSelfTest();
                return;
            }

            Options options = parseOptions(args);
            List<ArtifactResult> results = new ArrayList<>();
            for (Path artifact : options.artifacts()) {
                results.add(verifyArtifact(artifact, options));
            }
            verifyManifestConsistency(results);

            for (ArtifactResult result : results) {
                String manifestSummary = result.manifest() == null
                    ? "manifest metadata not requested"
                    : "package=" + result.manifest().applicationId()
                        + ", versionCode=" + result.manifest().versionCode()
                        + ", versionName=" + result.manifest().versionName();
                System.out.printf(
                    Locale.ROOT,
                    "PASS %s: billing=%s (%s); %s%n",
                    result.type().toUpperCase(Locale.ROOT),
                    result.billingVersion(),
                    result.billingEntry(),
                    manifestSummary
                );
            }
        } catch (Exception error) {
            System.err.println("FAIL: " + error.getMessage());
            System.exit(1);
        }
    }

    private static Options parseOptions(String[] args) {
        String minimumBilling = DEFAULT_MINIMUM_BILLING;
        String expectedBilling = null;
        Integer minimumVersionCode = null;
        Integer expectedVersionCode = null;
        String expectedVersionName = null;
        String expectedPackage = null;
        Path bundletool = null;
        List<Path> artifacts = new ArrayList<>();

        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            switch (arg) {
                case "--minimum-billing" -> minimumBilling = requireValue(args, ++index, arg);
                case "--expected-billing" -> expectedBilling = requireValue(args, ++index, arg);
                case "--minimum-version-code" -> minimumVersionCode = Integer.valueOf(
                    requireValue(args, ++index, arg)
                );
                case "--expected-version-code" -> expectedVersionCode = Integer.valueOf(
                    requireValue(args, ++index, arg)
                );
                case "--expected-version-name" -> expectedVersionName = requireValue(
                    args,
                    ++index,
                    arg
                );
                case "--expected-package" -> expectedPackage = requireValue(args, ++index, arg);
                case "--bundletool" -> bundletool = Path.of(requireValue(args, ++index, arg));
                default -> {
                    if (arg.startsWith("--")) {
                        throw new IllegalArgumentException("unknown option: " + arg);
                    }
                    artifacts.add(Path.of(arg));
                }
            }
        }

        parseVersion(minimumBilling);
        if (expectedBilling != null) parseVersion(expectedBilling);
        if (artifacts.isEmpty()) {
            throw new IllegalArgumentException(
                "usage: java scripts/CheckAndroidBillingVersion.java [options] <apk|aab>..."
            );
        }
        return new Options(
            minimumBilling,
            expectedBilling,
            minimumVersionCode,
            expectedVersionCode,
            expectedVersionName,
            expectedPackage,
            bundletool,
            List.copyOf(artifacts)
        );
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException(option + " requires a value");
        }
        return args[index];
    }

    private static ArtifactResult verifyArtifact(Path artifact, Options options) throws Exception {
        if (!Files.isRegularFile(artifact)) {
            throw new IllegalArgumentException("artifact is not a regular file: " + artifact);
        }
        String filename = artifact.getFileName().toString().toLowerCase(Locale.ROOT);
        String type = filename.endsWith(".apk") ? "apk" : filename.endsWith(".aab") ? "aab" : null;
        if (type == null) {
            throw new IllegalArgumentException("artifact must end in .apk or .aab: " + artifact);
        }

        try (ZipFile zip = new ZipFile(artifact.toFile())) {
            boolean apkStructure = zip.getEntry("AndroidManifest.xml") != null
                && zip.getEntry("BundleConfig.pb") == null;
            boolean aabStructure = zip.getEntry("BundleConfig.pb") != null
                && zip.getEntry("base/manifest/AndroidManifest.xml") != null;
            if (("apk".equals(type) && !apkStructure) || ("aab".equals(type) && !aabStructure)) {
                throw new IllegalArgumentException(
                    "archive structure does not match ." + type + " extension: " + artifact
                );
            }

            String expectedBillingEntry = "apk".equals(type)
                ? "billing.properties"
                : "base/root/billing.properties";
            List<? extends ZipEntry> billingEntries = zip.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().equals("billing.properties")
                    || entry.getName().endsWith("/billing.properties"))
                .toList();
            if (billingEntries.size() != 1
                || !expectedBillingEntry.equals(billingEntries.get(0).getName())) {
                throw new IllegalArgumentException(
                    artifact + " must contain exactly " + expectedBillingEntry
                        + " and no other billing.properties entries; found "
                        + billingEntries.stream().map(ZipEntry::getName).toList()
                );
            }

            ZipEntry billingEntry = billingEntries.get(0);
            String properties = readSmallEntry(zip, billingEntry, MAX_BILLING_PROPERTIES_BYTES);
            Matcher keyMatch = BILLING_KEY.matcher(properties);
            int keyCount = 0;
            while (keyMatch.find()) keyCount++;
            if (keyCount != 1) {
                throw new IllegalArgumentException(
                    billingEntry.getName() + " contains " + keyCount
                        + " billing_client keys; expected exactly one"
                );
            }
            Matcher match = BILLING_VERSION.matcher(properties);
            if (!match.find()) {
                throw new IllegalArgumentException(
                    "billing_client version is unreadable in " + billingEntry.getName()
                );
            }
            String billingVersion = match.group(1);
            if (compareVersions(billingVersion, options.minimumBilling()) < 0) {
                throw new IllegalArgumentException(
                    artifact + " contains Play Billing " + billingVersion
                        + "; required minimum is " + options.minimumBilling()
                );
            }
            if (options.expectedBilling() != null
                && !options.expectedBilling().equals(billingVersion)) {
                throw new IllegalArgumentException(
                    artifact + " contains Play Billing " + billingVersion
                        + "; expected exactly " + options.expectedBilling()
                );
            }

            ManifestMetadata manifest = null;
            if ("apk".equals(type)) {
                ZipEntry manifestEntry = zip.getEntry("AndroidManifest.xml");
                byte[] bytes = readSmallEntryBytes(zip, manifestEntry, MAX_MANIFEST_BYTES);
                manifest = parseBinaryManifest(bytes);
                verifyManifest(manifest, options, artifact);
            } else if (options.bundletool() != null) {
                manifest = parseAabManifest(artifact, options.bundletool());
                verifyManifest(manifest, options, artifact);
            } else if (requiresManifest(options)) {
                throw new IllegalArgumentException(
                    "AAB version/package assertions require --bundletool <bundletool-all.jar>"
                );
            }
            return new ArtifactResult(
                artifact,
                type,
                billingEntry.getName(),
                billingVersion,
                manifest
            );
        }
    }

    private static void verifyManifest(
        ManifestMetadata manifest,
        Options options,
        Path artifact
    ) {
        if (options.minimumVersionCode() != null
            && manifest.versionCode() < options.minimumVersionCode()) {
            throw new IllegalArgumentException(
                artifact + " has versionCode " + manifest.versionCode()
                    + "; minimum is " + options.minimumVersionCode()
            );
        }
        if (options.expectedVersionCode() != null
            && manifest.versionCode() != options.expectedVersionCode()) {
            throw new IllegalArgumentException(
                artifact + " has versionCode " + manifest.versionCode()
                    + "; expected " + options.expectedVersionCode()
            );
        }
        if (options.expectedVersionName() != null
            && !options.expectedVersionName().equals(manifest.versionName())) {
            throw new IllegalArgumentException(
                artifact + " has versionName " + manifest.versionName()
                    + "; expected " + options.expectedVersionName()
            );
        }
        if (options.expectedPackage() != null
            && !options.expectedPackage().equals(manifest.applicationId())) {
            throw new IllegalArgumentException(
                artifact + " has package " + manifest.applicationId()
                    + "; expected " + options.expectedPackage()
            );
        }
    }

    private static boolean requiresManifest(Options options) {
        return options.minimumVersionCode() != null
            || options.expectedVersionCode() != null
            || options.expectedVersionName() != null
            || options.expectedPackage() != null;
    }

    private static void verifyManifestConsistency(List<ArtifactResult> results) {
        ManifestMetadata apk = null;
        ManifestMetadata aab = null;
        for (ArtifactResult result : results) {
            if (result.manifest() == null) continue;
            if ("apk".equals(result.type())) {
                if (apk != null && !apk.equals(result.manifest())) {
                    throw new IllegalArgumentException("APK artifacts have different manifests");
                }
                apk = result.manifest();
            } else {
                if (aab != null && !aab.equals(result.manifest())) {
                    throw new IllegalArgumentException("AAB artifacts have different manifests");
                }
                aab = result.manifest();
            }
        }
        if (apk != null && aab != null && !apk.equals(aab)) {
            throw new IllegalArgumentException(
                "APK/AAB manifest mismatch: APK=" + apk + ", AAB=" + aab
            );
        }
    }

    private static String readSmallEntry(ZipFile zip, ZipEntry entry, int limit) throws IOException {
        return new String(readSmallEntryBytes(zip, entry, limit), StandardCharsets.UTF_8);
    }

    private static byte[] readSmallEntryBytes(ZipFile zip, ZipEntry entry, int limit)
        throws IOException {
        if (entry == null) throw new IllegalArgumentException("required ZIP entry is missing");
        if (entry.getSize() > limit) {
            throw new IllegalArgumentException(entry.getName() + " exceeds " + limit + " bytes");
        }
        try (var input = zip.getInputStream(entry)) {
            byte[] bytes = input.readNBytes(limit + 1);
            if (bytes.length > limit) {
                throw new IllegalArgumentException(entry.getName() + " exceeds " + limit + " bytes");
            }
            return bytes;
        }
    }

    private static ManifestMetadata parseAabManifest(Path aab, Path bundletool) throws Exception {
        Path bundletoolJar = bundletool.toAbsolutePath().normalize();
        if (!Files.isRegularFile(bundletoolJar)) {
            throw new IllegalArgumentException(
                "bundletool is not a regular file: " + bundletoolJar
            );
        }
        Path javaExecutable = Path.of(
            System.getProperty("java.home"),
            "bin",
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
                ? "java.exe"
                : "java"
        );
        Process process = new ProcessBuilder(
            javaExecutable.toString(),
            "-jar",
            bundletoolJar.toString(),
            "dump",
            "manifest",
            "--bundle=" + aab.toAbsolutePath().normalize(),
            "--module=base"
        ).start();

        BoundedStreamReader stdout = new BoundedStreamReader(
            process.getInputStream(),
            MAX_MANIFEST_BYTES
        );
        BoundedStreamReader stderr = new BoundedStreamReader(
            process.getErrorStream(),
            MAX_BUNDLETOOL_ERROR_BYTES
        );
        Thread stdoutThread = new Thread(stdout, "bundletool-stdout");
        Thread stderrThread = new Thread(stderr, "bundletool-stderr");
        stdoutThread.setDaemon(true);
        stderrThread.setDaemon(true);
        stdoutThread.start();
        stderrThread.start();

        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            throw new IllegalArgumentException("bundletool manifest dump timed out after 30 seconds");
        }
        stdoutThread.join(5000);
        stderrThread.join(5000);
        if (stdoutThread.isAlive() || stderrThread.isAlive()) {
            throw new IllegalArgumentException("bundletool output streams did not close cleanly");
        }
        byte[] stdoutBytes = stdout.bytes("bundletool stdout");
        byte[] stderrBytes = stderr.bytes("bundletool stderr");
        if (process.exitValue() != 0) {
            throw new IllegalArgumentException(
                "bundletool manifest dump failed with exit " + process.exitValue()
                    + ": " + new String(stderrBytes, StandardCharsets.UTF_8).strip()
            );
        }
        if (stdoutBytes.length == 0) {
            throw new IllegalArgumentException("bundletool returned an empty manifest");
        }
        return parseDecodedManifest(stdoutBytes);
    }

    private static ManifestMetadata parseDecodedManifest(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());
            var document = builder.parse(new ByteArrayInputStream(xml));
            var manifest = document.getDocumentElement();
            if (!"manifest".equals(manifest.getTagName())) {
                throw new IllegalArgumentException("bundletool XML root is not <manifest>");
            }
            String applicationId = manifest.getAttribute("package");
            String versionCodeText = manifest.getAttributeNS(ANDROID_NAMESPACE, "versionCode");
            String versionName = manifest.getAttributeNS(ANDROID_NAMESPACE, "versionName");
            if (applicationId.isBlank() || versionCodeText.isBlank() || versionName.isBlank()) {
                throw new IllegalArgumentException(
                    "bundletool manifest is missing package/version metadata"
                );
            }
            int versionCode;
            try {
                versionCode = Integer.parseInt(versionCodeText);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException(
                    "bundletool manifest has invalid versionCode: " + versionCodeText,
                    error
                );
            }
            return new ManifestMetadata(versionCode, versionName, applicationId);
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException(
                "cannot parse bundletool manifest XML: " + error.getMessage(),
                error
            );
        }
    }

    private static final class BoundedStreamReader implements Runnable {
        private final InputStream input;
        private final int limit;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private volatile IOException error;
        private volatile boolean tooLarge;

        private BoundedStreamReader(InputStream input, int limit) {
            this.input = input;
            this.limit = limit;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[8192];
            try (InputStream stream = input) {
                for (int read; (read = stream.read(buffer)) != -1; ) {
                    if (output.size() + read <= limit) {
                        output.write(buffer, 0, read);
                    } else {
                        tooLarge = true;
                    }
                }
            } catch (IOException caught) {
                error = caught;
            }
        }

        private byte[] bytes(String label) throws IOException {
            if (error != null) throw error;
            if (tooLarge) {
                throw new IllegalArgumentException(label + " exceeds " + limit + " bytes");
            }
            return output.toByteArray();
        }
    }

    private static ManifestMetadata parseBinaryManifest(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (bytes.length < 8 || unsignedShort(buffer, 0) != 0x0003) {
            throw new IllegalArgumentException("AndroidManifest.xml is not compiled binary XML");
        }
        int stringPoolOffset = 8;
        StringPool pool = StringPool.read(buffer, stringPoolOffset);
        int position = stringPoolOffset + intAt(buffer, stringPoolOffset + 4);
        Map<Integer, Integer> resourceIds = new HashMap<>();

        for (int cursor = position; cursor <= bytes.length - 8; ) {
            int type = unsignedShort(buffer, cursor);
            int size = intAt(buffer, cursor + 4);
            if (size <= 0 || cursor + size > bytes.length) break;
            if (type == 0x0180) {
                for (int index = 0; index < (size - 8) / 4; index++) {
                    resourceIds.put(index, intAt(buffer, cursor + 8 + index * 4));
                }
            }
            cursor += size;
        }

        Integer versionCode = null;
        String versionName = null;
        String applicationId = null;
        for (int cursor = position; cursor <= bytes.length - 8; ) {
            int type = unsignedShort(buffer, cursor);
            int size = intAt(buffer, cursor + 4);
            if (size <= 0 || cursor + size > bytes.length) break;
            if (type == 0x0102) {
                String elementName = pool.get(intAt(buffer, cursor + 20));
                int attributeCount = unsignedShort(buffer, cursor + 28);
                int attributeSize = unsignedShort(buffer, cursor + 26);
                int attributeOffset = cursor + 16 + unsignedShort(buffer, cursor + 24);
                for (int index = 0; index < attributeCount; index++) {
                    int attribute = attributeOffset + index * attributeSize;
                    int nameIndex = intAt(buffer, attribute + 4);
                    int resourceId = resourceIds.getOrDefault(nameIndex, 0);
                    String name = pool.get(nameIndex);
                    String value = attributeValue(buffer, pool, attribute);
                    if (resourceId == RES_VERSION_CODE) versionCode = Integer.valueOf(value);
                    if (resourceId == RES_VERSION_NAME) versionName = value;
                    if ("manifest".equals(elementName) && "package".equals(name)) {
                        applicationId = value;
                    }
                }
                if ("manifest".equals(elementName)) break;
            }
            cursor += size;
        }

        if (versionCode == null || versionName == null || applicationId == null) {
            throw new IllegalArgumentException(
                "compiled manifest is missing package/version metadata"
            );
        }
        return new ManifestMetadata(versionCode, versionName, applicationId);
    }

    private static String attributeValue(ByteBuffer buffer, StringPool pool, int offset) {
        int rawIndex = intAt(buffer, offset + 8);
        if (rawIndex != -1) return pool.get(rawIndex);
        int dataType = Byte.toUnsignedInt(buffer.get(offset + 15));
        int data = intAt(buffer, offset + 16);
        if (dataType == 0x03) return pool.get(data);
        return Integer.toString(data);
    }

    private static int unsignedShort(ByteBuffer buffer, int offset) {
        return Short.toUnsignedInt(buffer.getShort(offset));
    }

    private static int intAt(ByteBuffer buffer, int offset) {
        return buffer.getInt(offset);
    }

    private static final class StringPool {
        private final ByteBuffer buffer;
        private final int count;
        private final int stringsOffset;
        private final int[] offsets;
        private final boolean utf8;

        private StringPool(
            ByteBuffer buffer,
            int count,
            int stringsOffset,
            int[] offsets,
            boolean utf8
        ) {
            this.buffer = buffer;
            this.count = count;
            this.stringsOffset = stringsOffset;
            this.offsets = offsets;
            this.utf8 = utf8;
        }

        static StringPool read(ByteBuffer buffer, int offset) {
            int count = intAt(buffer, offset + 8);
            boolean utf8 = (intAt(buffer, offset + 16) & (1 << 8)) != 0;
            int stringsOffset = offset + intAt(buffer, offset + 20);
            int[] offsets = new int[count];
            for (int index = 0; index < count; index++) {
                offsets[index] = intAt(buffer, offset + 28 + index * 4);
            }
            return new StringPool(buffer, count, stringsOffset, offsets, utf8);
        }

        String get(int index) {
            if (index < 0 || index >= count) return null;
            int cursor = stringsOffset + offsets[index];
            if (utf8) {
                cursor = skipLength8(buffer, cursor);
                Length utf8Length = length8(buffer, cursor);
                cursor += utf8Length.bytes();
                byte[] value = new byte[utf8Length.value()];
                ByteBuffer duplicate = buffer.duplicate();
                duplicate.position(cursor);
                duplicate.get(value);
                return new String(value, StandardCharsets.UTF_8);
            }
            Length utf16Length = length16(buffer, cursor);
            cursor += utf16Length.bytes();
            byte[] value = new byte[utf16Length.value() * 2];
            ByteBuffer duplicate = buffer.duplicate();
            duplicate.position(cursor);
            duplicate.get(value);
            return new String(value, StandardCharsets.UTF_16LE);
        }

        private static int skipLength8(ByteBuffer buffer, int offset) {
            return offset + length8(buffer, offset).bytes();
        }

        private static Length length8(ByteBuffer buffer, int offset) {
            int first = Byte.toUnsignedInt(buffer.get(offset));
            if ((first & 0x80) == 0) return new Length(first, 1);
            int second = Byte.toUnsignedInt(buffer.get(offset + 1));
            return new Length(((first & 0x7f) << 8) | second, 2);
        }

        private static Length length16(ByteBuffer buffer, int offset) {
            int first = unsignedShort(buffer, offset);
            if ((first & 0x8000) == 0) return new Length(first, 2);
            int second = unsignedShort(buffer, offset + 2);
            return new Length(((first & 0x7fff) << 16) | second, 4);
        }

        private record Length(int value, int bytes) {}
    }

    private static int compareVersions(String left, String right) {
        int[] a = parseVersion(left);
        int[] b = parseVersion(right);
        for (int index = 0; index < 3; index++) {
            int comparison = Integer.compare(a[index], b[index]);
            if (comparison != 0) return comparison;
        }
        return 0;
    }

    private static int[] parseVersion(String value) {
        if (value == null || !value.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) {
            throw new IllegalArgumentException("invalid semantic version: " + value);
        }
        String[] parts = value.split("\\.");
        return new int[] {
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]),
        };
    }

    private static void runSelfTest() throws Exception {
        if (compareVersions("8.0.0", "8.0.0") != 0
            || compareVersions("9.1.0", "8.0.0") <= 0
            || compareVersions("7.9.9", "8.0.0") >= 0) {
            throw new AssertionError("semantic version comparison failed");
        }

        Path temp = Files.createTempDirectory("billing-artifact-verifier-");
        try {
            Path apk = temp.resolve("valid.apk");
            writeZip(apk, Map.of(
                "AndroidManifest.xml", new byte[] {3, 0, 8, 0, 8, 0, 0, 0},
                "billing.properties", "billing_client=9.1.0\n".getBytes(StandardCharsets.UTF_8)
            ));
            Path aab = temp.resolve("valid.aab");
            writeZip(aab, Map.of(
                "BundleConfig.pb", new byte[] {0},
                "base/manifest/AndroidManifest.xml", new byte[] {0},
                "base/root/billing.properties",
                    "billing_client=8.0.0\n".getBytes(StandardCharsets.UTF_8)
            ));
            Path old = temp.resolve("old.aab");
            writeZip(old, Map.of(
                "BundleConfig.pb", new byte[] {0},
                "base/manifest/AndroidManifest.xml", new byte[] {0},
                "base/root/billing.properties",
                    "billing_client=7.1.1\n".getBytes(StandardCharsets.UTF_8)
            ));
            Path missing = temp.resolve("missing.aab");
            writeZip(missing, Map.of(
                "BundleConfig.pb", new byte[] {0},
                "base/manifest/AndroidManifest.xml", new byte[] {0}
            ));
            Path duplicate = temp.resolve("duplicate.aab");
            writeZip(duplicate, Map.of(
                "BundleConfig.pb", new byte[] {0},
                "base/manifest/AndroidManifest.xml", new byte[] {0},
                "base/root/billing.properties",
                    "billing_client=9.1.0\n".getBytes(StandardCharsets.UTF_8),
                "other/billing.properties",
                    "billing_client=9.1.0\n".getBytes(StandardCharsets.UTF_8)
            ));
            Path duplicateKey = temp.resolve("duplicate-key.aab");
            writeZip(duplicateKey, Map.of(
                "BundleConfig.pb", new byte[] {0},
                "base/manifest/AndroidManifest.xml", new byte[] {0},
                "base/root/billing.properties",
                    "billing_client=9.1.0\nbilling_client=9.1.0\n"
                        .getBytes(StandardCharsets.UTF_8)
            ));
            Path wrongPath = temp.resolve("wrong-path.aab");
            writeZip(wrongPath, Map.of(
                "BundleConfig.pb", new byte[] {0},
                "base/manifest/AndroidManifest.xml", new byte[] {0},
                "assets/billing.properties",
                    "billing_client=9.1.0\n".getBytes(StandardCharsets.UTF_8)
            ));

            Options minimumOnly = new Options(
                "8.0.0", null, null, null, null, null, null, List.of(aab)
            );
            verifyArtifact(aab, minimumOnly);
            expectFailure(() -> verifyArtifact(old, minimumOnly), "old billing");
            expectFailure(() -> verifyArtifact(missing, minimumOnly), "missing billing");
            expectFailure(() -> verifyArtifact(duplicate, minimumOnly), "duplicate billing");
            expectFailure(() -> verifyArtifact(duplicateKey, minimumOnly), "duplicate key");
            expectFailure(() -> verifyArtifact(wrongPath, minimumOnly), "wrong billing path");

            // Structure is validated before the deliberately minimal synthetic APK manifest.
            Options expected = new Options(
                "8.0.0", "8.3.0", null, null, null, null, null, List.of(apk)
            );
            expectFailure(() -> verifyArtifact(apk, expected), "unexpected billing");

            byte[] decodedXml = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<manifest xmlns:android=\"" + ANDROID_NAMESPACE + "\" "
                + "package=\"com.simonykim.ejectbutton\" "
                + "android:versionCode=\"1089\" android:versionName=\"1.7.3\"/>")
                .getBytes(StandardCharsets.UTF_8);
            ManifestMetadata decoded = parseDecodedManifest(decodedXml);
            if (!decoded.equals(new ManifestMetadata(1089, "1.7.3", "com.simonykim.ejectbutton"))) {
                throw new AssertionError("decoded manifest parsing failed: " + decoded);
            }
            byte[] maliciousXml = ("<?xml version=\"1.0\"?>"
                + "<!DOCTYPE manifest [<!ENTITY xxe SYSTEM \"file:///should-not-be-read\">]>"
                + "<manifest xmlns:android=\"" + ANDROID_NAMESPACE + "\" "
                + "package=\"&xxe;\" android:versionCode=\"1089\" "
                + "android:versionName=\"1.7.3\"/>").getBytes(StandardCharsets.UTF_8);
            expectFailure(() -> parseDecodedManifest(maliciousXml), "XML external entity");

            ArtifactResult apkResult = new ArtifactResult(
                Path.of("same.apk"), "apk", "billing.properties", "9.1.0", decoded
            );
            ArtifactResult aabResult = new ArtifactResult(
                Path.of("same.aab"), "aab", "base/root/billing.properties", "9.1.0", decoded
            );
            verifyManifestConsistency(List.of(apkResult, aabResult));
            ArtifactResult mismatchedAab = new ArtifactResult(
                Path.of("old.aab"),
                "aab",
                "base/root/billing.properties",
                "9.1.0",
                new ManifestMetadata(1088, "1.7.2", "com.simonykim.ejectbutton")
            );
            expectFailure(
                () -> verifyManifestConsistency(List.of(apkResult, mismatchedAab)),
                "APK/AAB mismatch"
            );
            System.out.println("self-test: PASS (14 checks)");
        } finally {
            try (var paths = Files.walk(temp)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException error) {
                        throw new RuntimeException(error);
                    }
                });
            }
        }
    }

    private static void writeZip(Path path, Map<String, byte[]> entries) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(path))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
    }

    private static void expectFailure(ThrowingRunnable runnable, String label) throws Exception {
        try {
            runnable.run();
            throw new AssertionError(label + " should fail");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
