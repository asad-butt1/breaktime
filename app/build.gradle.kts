plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.paparazzi")
}

android {
    namespace = "com.asad.breaktime"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.asad.breaktime"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "0.8.0"

        // AdMob. The App ID is always the real BreakTime one (it only identifies the app).
        // The ad unit is Google's TEST unit unless USE_REAL_ADS is true, so testers never
        // see or click real ads. Flip USE_REAL_ADS to true only for the public production release.
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3984378567292938~4464314911"
        val useRealAds = false
        val adUnit = if (useRealAds) "ca-app-pub-3984378567292938/5077403488" else "ca-app-pub-3940256099942544/1033173712"
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$adUnit\"")
    }

    // Release signing comes from environment variables (set as GitHub secrets in CI).
    val keystorePath = System.getenv("BT_KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("BT_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("BT_KEY_ALIAS")
                keyPassword = System.getenv("BT_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.android.billingclient:billing:7.1.1")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
