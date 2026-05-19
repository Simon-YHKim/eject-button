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
        // v1.6.7 — Native ad → Banner ad 정식 image-only 형식 교체.
        //   Native ad code-side video filter (v1.6.6) 는 사용자에게 image 만 노출했지만
        //   validator 는 80dp MediaView 사이즈만 보고 warning. Banner 는 구조적으로
        //   video 미수신 → warning 0 + UX 동일 image-only 결과.
        // v1.6.8 — AdMob 콘솔에서 Native + Rewarded 광고 단위 영구 삭제.
        //   secrets.properties / GitHub Actions workflow 에서도 ADMOB_NATIVE_ID,
        //   ADMOB_REWARDED_ID 참조 제거. 활성 광고 단위: Banner + Interstitial 만.
        buildConfigField("String", "ADMOB_BANNER_ID",
            "\"${secretsProps.getProperty("ADMOB_BANNER_ID")
                ?: if (isReleaseTask) error("ADMOB_BANNER_ID missing - release refuses test fallback")
                   else "ca-app-pub-3940256099942544/6300978111"}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID",
            "\"${secretsProps.getProperty("ADMOB_INTERSTITIAL_ID")
                ?: if (isReleaseTask) error("ADMOB_INTERSTITIAL_ID missing ??release refuses test fallback")
                   else "ca-app-pub-3940256099942544/1033173712"}\"")
        // v1.1.0 ~ v1.6.5: ADMOB_REWARDED_ID was used by RewardedAdDialog (30s video ad for 1-caller unlock).
        // v1.6.6: replaced by share-to-unlock model (one app share = permanent unlock). Rewarded code + secret removed.
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
    // v1.6.11 — In-App Update (Flexible flow). 사용자 수동 갱신 의존 제거.
    implementation(libs.play.app.update)
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
    // v1.6.11 — kotlin-reflect: AppStringsCompletenessTest 가 7-locale × N-field
    //   reflection iteration 으로 빈 문자열 검출. test 전용이라 release AAB 사이즈 영향 X.
    // v1.7.1 — 버전을 project Kotlin compiler (2.0.0, libs.versions.toml) 와 정확히
    //   일치시킴. 이전 2.0.21 는 ClassFormatError 유발 → testDebugUnitTest 4건 실패
    //   (AppStringsCompletenessTest 3 + ScenarioRuntimeTest 1) → release-aab 빌드 차단.
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:2.0.0")
}
