import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.bits.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bits.todoandgames"
        minSdk = 26
        targetSdk = 36
        // Bumped to match everything shipped since 1.4.1: Bitris, Spasa, Chess, pause
        // controls, the widget responsiveness fix, and the chess footer fixes. Every
        // Play Store upload needs a versionCode strictly higher than the last one it
        // accepts, so this has to move before the first real release goes up.
        versionCode = 25
        versionName = "1.8"
    }

    // A fixed debug key, so each new build installs over the previous one
    // without uninstalling first (uninstalling would erase your lists).
    signingConfigs {
        getByName("debug") {
            storeFile = file("bits-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        // Release signing details live in keystore.properties, which is gitignored and
        // never committed. If that file is absent (for example on a machine that only
        // builds debug), the release config is simply left unconfigured rather than
        // failing the whole build.
        create("release") {
            val keystoreProperties = Properties()
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            // Deliberately NO applicationIdSuffix: adding one would make debug builds
            // install as a separate app, and the lists in the existing install would
            // appear to vanish.
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            // R8 shrinks and obfuscates. Bits has no reflection-based code, so the
            // default rules plus the Compose/Glance consumer rules are enough.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
        // Needed so BuildConfig.DEBUG can gate the developer-only settings.
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    // animateColorAsState, AnimatedVisibility, updateTransition and friends live here.
    // material3/foundation happened to pull in enough of this for some of it to work,
    // but not all of it, hence the inconsistent errors — declaring it directly removes
    // that ambiguity.
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // Home screen widget
    implementation("androidx.glance:glance-appwidget:1.1.1")

    // Drag-and-drop reordering
    implementation("sh.calvin.reorderable:reorderable:2.4.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Google Play Billing, for the one-time Lifetime purchase and the monthly plan.
    //
    // Must stay on 8.x or newer: Play stopped accepting uploads built against Billing 7
    // on 31 August 2026, and version 8 is required until 31 August 2027. This is checked
    // at upload time against the library bundled in the AAB, so it applies even while
    // Monetization.ENABLED is false and nothing is actually for sale.
    //
    // The plain artifact rather than billing-ktx: PlayProStore uses the callback API
    // throughout and needs none of the coroutine extensions.
    implementation("com.android.billingclient:billing:8.0.0")
}
