import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.c200.ayushkaranjkar"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.c200.ayushkaranjkar"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // ✅ Load from local.properties (no imports needed)
        val localProps = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localProps.load(localFile.inputStream())
        }
        val geminiApiKey: String = localProps.getProperty("GEMINI_API_KEY") ?: ""

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.generativeai)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}