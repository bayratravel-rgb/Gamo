plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.bayera.travel.common"
    compileSdk = 34
    defaultConfig { minSdk = 24 }

    // --- FIX: Force Java 17 for Java Compiler ---
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // --- FIX: Force Java 17 for Kotlin Compiler ---
    kotlinOptions {
        jvmTarget = "17"
    }
}
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}
