// Move imports to the top
import org.gradle.kotlin.dsl.invoke
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Add this line for the Compose Compiler plugin
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt") // For Room and AutoValue

    // Apply the google-services plugin - this is the only line you need for it here
    id("com.google.gms.google-services")
}
// REMOVE THIS LINE: apply(plugin = "com.google.gms.google-services")

android {
    namespace = "com.example.truthnudge" // Replace with your actual namespace
    compileSdk = 36 // Updated from 33

    defaultConfig {
        applicationId = "com.example.truthnudge" // Replace with your actual application ID
        minSdk = 24 // Or your desired minimum SDK
        targetSdk = 34 // Consider updating this as well
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

    // ... other configurations

        // Logic for GEMINI_API_KEY
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { input ->
                localProperties.load(input)
            }
        }
        val geminiKey = localProperties.getProperty("GEMINI_API_KEY", "")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}
// ... rest of the file

 // This is the correct closing brace for the android block

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) // Correct BOM usage
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.appcompat) // Assuming you have an alias for this
    implementation(libs.androidx.constraintlayout) // Assuming you have an alias

    // Gemini AI Client - Assuming you have an alias 'libs.gemini.ai.client' or similar
    implementation(libs.gemini.ai.client) // Or keep as "com.google.ai.client.generativeai:generativeai:0.9.0" if not in TOML

    // AutoValue
    implementation("com.google.auto.value:auto-value-annotations:1.11.0")
    kapt("com.google.auto.value:auto-value:1.11.0")

    // AndroidX Credentials & Google Identity
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth) // Keep this if needed for Credential Manager or other Google services

    // Firebase
    // Import the Firebase BoM using your alias from libs.versions.toml
    implementation(platform("com.google.firebase:firebase-bom:32.7.4")) // Use the latest version


    // Add specific Firebase KTX libraries using your aliases
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.database) // This will use the KTX version if managed by BOM
    implementation(libs.firebase.firestore.ktx)
    // REMOVE: implementation("com.google.firebase:firebase-auth")
    // REMOVE: implementation("com.google.firebase:firebase-analytics")
    // REMOVE: implementation("com.google.firebase:firebase-ktx") // General KTX might be redundant if specific ones are used

    // Room
    implementation(libs.androidx.room.runtime) // Assuming aliases like these exist from your TOML
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Material
    implementation(libs.material)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core) // Check alias
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
// ... other dependencies
    implementation("com.google.android.material:material:1.12.0")
//
    // Checker Framework
    implementation("org.checkerframework:checker-qual:3.50.0")
    // REMOVE: implementation("com.google.android.gms:play-services-auth") // Redundant
}



configurations.all {
    resolutionStrategy {
        force("org.checkerframework:checker-qual:3.50.0")
        ("com.google.android.material:material:1.12.0")
    }
}
