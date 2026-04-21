import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Load keystore properties if available
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.grantlittman.wearapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.grantlittman.pulsetimer"
        minSdk = 30
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM — single version source for all Compose libraries
    val composeBom = platform("androidx.compose:compose-bom:2025.04.00")
    implementation(composeBom)

    // Wear OS Compose foundation & material
    implementation("androidx.wear.compose:compose-foundation:1.5.0")
    implementation("androidx.wear.compose:compose-material3:1.0.0-alpha36")
    implementation("androidx.wear.compose:compose-navigation:1.5.0")

    // Core Compose (pulled via BOM)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.runtime:runtime")

    // Activity & lifecycle
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")

    // Wear OS core (includes ambient mode support)
    implementation("androidx.wear:wear:1.4.0")
    implementation("androidx.wear:wear-ongoing:1.1.0")

    // Watch face complication data source
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")

    // DataStore for pattern persistence
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // JSON serialization
    implementation(libs.gson)

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
}
