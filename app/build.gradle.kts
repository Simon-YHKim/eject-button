import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ejectbutton.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // secrets.properties에서 API 키 주입 (보안: 소스코드에 키 노출 방지)
        buildConfigField("String", "CLARITY_PROJECT_ID",
            "\"${secretsProps.getProperty("CLARITY_PROJECT_ID", "")}\"")
        buildConfigField("String", "ADMOB_BANNER_ID",
            "\"${secretsProps.getProperty("ADMOB_BANNER_ID", "ca-app-pub-3940256099942544/6300978111")}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID",
            "\"${secretsProps.getProperty("ADMOB_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/1033173712")}\"")
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
    debugImplementation(libs.androidx.ui.tooling)
}
