import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

// keystore.properties 濡쒕뱶 (?놁쑝硫?由대━利??쒕챸 ?ㅽ궢)
val keystoreProps = Properties().also { props ->
    val f = rootProject.file("keystore.properties")
    if (f.exists()) props.load(f.inputStream())
}

// secrets.properties 濡쒕뱶 (API ????git???ы븿?섏? ?딆쓬)
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

        // secrets.properties?먯꽌 API ??二쇱엯 (蹂댁븞: ?뚯뒪肄붾뱶?????몄텧 諛⑹?).
        //
        // Round 28 ??the previous fallbacks baked Google's AdMob TEST unit IDs
        // (ca-app-pub-3940256099942544/?? into any build where secrets.properties
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
                    "See SETUP_GUIDE.md ??secrets.properties."
                )
            }
            return value
        }
        buildConfigField("String", "CLARITY_PROJECT_ID",
            "\"${requireSecret("CLARITY_PROJECT_ID")}\"")
        buildConfigField("String", "ADMOB_NATIVE_ID",
            "\"${secretsProps.getProperty("ADMOB_NATIVE_ID")
                ?: if (isReleaseTask) error("ADMOB_NATIVE_ID missing ??release refuses test fallback")
                   else "ca-app-pub-3940256099942544/2247696110"}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID",
            "\"${secretsProps.getProperty("ADMOB_INTERSTITIAL_ID")
                ?: if (isReleaseTask) error("ADMOB_INTERSTITIAL_ID missing ??release refuses test fallback")
                   else "ca-app-pub-3940256099942544/1033173712"}\"")
        // v1.1.0 ??Rewarded Ad unit ID. RewardedAdDialog ?먯꽌 30珥?愿묎퀬 1???쒖껌
        // ???좉릿 湲곕뒫 1???ъ슜 沅뚰븳 遺?? ?붾쾭洹?fallback ? Google 怨듭떇 ?뚯뒪??ID.
        buildConfigField("String", "ADMOB_REWARDED_ID",
            "\"${secretsProps.getProperty("ADMOB_REWARDED_ID")
                ?: if (isReleaseTask) error("ADMOB_REWARDED_ID missing ??release refuses test fallback")
                   else "ca-app-pub-3940256099942544/5224354917"}\"")
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
    // v1.2 ??UMP SDK. GDPR consent flow. ConsentManager媛 wrapping.
    implementation(libs.google.ump)
    implementation(libs.ms.clarity)
    // Firebase ??BoM ?쇰줈 紐⑤뱺 Firebase ?쇱씠釉뚮윭由?踰꾩쟾????踰덉뿉 ?듭씪.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    // v1.0.10 ??Crashlytics 異붽?. ?먯껜 CrashReportManager (?ъ슜??硫붿씪 ?꾩넚 ?섏〈, 蹂닿퀬??1-5%)
    // ? 蹂묓뻾 ?댁쁺. Crashlytics ??100% ?먮룞 ?대씪?곕뱶 ?섏쭛 ??誘몃컻寃??щ옒??利됱떆 ?몄?.
    implementation(libs.firebase.crashlytics)
    // v1.5.10 — Removed Geofencing dependency (libs.google.location).
    //   Reason: v1.5.8 patch removed all GPS-trigger features after Play Console rejected
    //   ACCESS_FINE_LOCATION + ACCESS_BACKGROUND_LOCATION. Confirmed 0 usages in code.
    // v1.4.2 — Lottie for drag-to-confirm rings overlay.
    implementation(libs.lottie.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Unit tests ??JVM only, no Android runtime required.
    // Added in Round 27 so we can regression-test the emoji/surrogate-pair
    // history parser, callerLabel formatting, and future pure-Kotlin logic.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
}
