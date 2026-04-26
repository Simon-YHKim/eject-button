import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

// keystore.properties 로드 (없으면 릴리즈 서명 스킵)
val keystoreProps = Properties().also { props ->
    val f = rootProject.file("keystore.properties")
    if (f.exists()) props.load(f.inputStream())
}

// secrets.properties 로드 (API 키 — git에 포함되지 않음)
val secretsProps = Properties().also { props ->
    val f = rootProject.file("secrets.properties")
    if (f.exists()) props.load(f.inputStream())
}

android {
    namespace = "com.ejectbutton"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.simonykim.ejectbutton"
        minSdk = 26
        targetSdk = 35
        // versionCode is overridden by CI via -PversionCodeOverride=<int>.
        // See .github/workflows/release-aab.yml (uses GITHUB_RUN_NUMBER + offset).
        versionCode = (project.findProperty("versionCodeOverride") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("versionNameOverride") as String?) ?: "1.0"

        // secrets.properties에서 API 키 주입 (보안: 소스코드에 키 노출 방지).
        //
        // Round 28 — the previous fallbacks baked Google's AdMob TEST unit IDs
        // (ca-app-pub-3940256099942544/…) into any build where secrets.properties
        // was missing. A release APK that ships with test IDs causes AdMob to
        // suspend the publisher account. We now fail-fast at configuration time
        // when a release build can't find the real IDs; debug builds keep the
        // test IDs as a developer convenience.
        val isReleaseTask = gradle.startParameter.taskNames.any {
            it.contains("Release", ignoreCase = true) || it.startsWith(":app:bundle", ignoreCase = true)
        }
        fun requireSecret(key: String): String {
            val value = secretsProps.getProperty(key, "")
            if (value.isBlank() && isReleaseTask) {
                error(
                    "Required secret '$key' is missing from keystore/secrets.properties. " +
                    "Release builds refuse to fall back to placeholder values. " +
                    "See SETUP_GUIDE.md → secrets.properties."
                )
            }
            return value
        }
        buildConfigField("String", "CLARITY_PROJECT_ID",
            "\"${requireSecret("CLARITY_PROJECT_ID")}\"")
        buildConfigField("String", "ADMOB_NATIVE_ID",
            "\"${secretsProps.getProperty("ADMOB_NATIVE_ID")
                ?: if (isReleaseTask) error("ADMOB_NATIVE_ID missing — release refuses test fallback")
                   else "ca-app-pub-3940256099942544/2247696110"}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID",
            "\"${secretsProps.getProperty("ADMOB_INTERSTITIAL_ID")
                ?: if (isReleaseTask) error("ADMOB_INTERSTITIAL_ID missing — release refuses test fallback")
                   else "ca-app-pub-3940256099942544/1033173712"}\"")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile     = file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias      = keystoreProps["keyAlias"] as String
                keyPassword   = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // False positive: rule targets old FragmentActivity, not ComponentActivity
        disable += "InvalidFragmentVersionForActivityResult"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.play.review)
    implementation(libs.play.billing)
    implementation(libs.google.admob)
    implementation(libs.ms.clarity)
    // Firebase — BoM 으로 모든 Firebase 라이브러리 버전을 한 번에 통일.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    // v1.0.10 — Crashlytics 추가. 자체 CrashReportManager (사용자 메일 전송 의존, 보고율 1-5%)
    // 와 병행 운영. Crashlytics 는 100% 자동 클라우드 수집 → 미발견 크래시 즉시 인지.
    implementation(libs.firebase.crashlytics)
    debugImplementation(libs.androidx.ui.tooling)

    // Unit tests — JVM only, no Android runtime required.
    // Added in Round 27 so we can regression-test the emoji/surrogate-pair
    // history parser, callerLabel formatting, and future pure-Kotlin logic.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
}
