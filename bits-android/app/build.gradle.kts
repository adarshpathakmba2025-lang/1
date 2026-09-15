plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.bits.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bits.todoandgames"
        minSdk = 26
        targetSdk = 35
        versionCode = 20
        versionName = "1.3.5"
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
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
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
}
