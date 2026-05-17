plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.featuredemo.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.featuredemo.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":android-sdk"))
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
