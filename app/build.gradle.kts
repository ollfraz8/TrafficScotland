
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Traffic Scotland API credentials
val secrets = Properties()
val secretsFile = rootProject.file("secrets.properties")

if (secretsFile.exists()) {
    secretsFile.inputStream().use {
        secrets.load(it)
    }
}

// Google Maps API key
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

android {
    namespace = "com.example.trafficscotland"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.trafficscotland"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        // Traffic Scotland API credentials
        buildConfigField(
            "String",
            "TRAFFIC_CLIENT_ID",
            "\"${secrets.getProperty("TRAFFIC_CLIENT_ID", "")}\""
        )

        buildConfigField(
            "String",
            "TRAFFIC_CLIENT_KEY",
            "\"${secrets.getProperty("TRAFFIC_CLIENT_KEY", "")}\""
        )

        // Google Maps API key
        manifestPlaceholders["MAPS_API_KEY"] =
            localProperties.getProperty("MAPS_API_KEY", "")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // MVVM architecture
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata:2.9.4")
    implementation("androidx.fragment:fragment:1.8.9")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:19.2.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}