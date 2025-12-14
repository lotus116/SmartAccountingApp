plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.smartaccountingapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.smartaccountingapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // MPAndroidChart for chart analysis
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // Gson for JSON serialization (Import/Export)
    implementation("com.google.code.gson:gson:2.10.1")
}