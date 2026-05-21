plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.featureflagsdk.android"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
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
    // HTTP: OkHttp + coroutines (to be added when implementing)
    // implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // JSON: kotlinx.serialization or Gson
    // implementation("com.google.code.gson:gson:2.11.0")
}
