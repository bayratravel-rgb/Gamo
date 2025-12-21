plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bayera.travel.utils"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // USE 'api' instead of 'implementation' so the classes are visible
    api(project(":packages:shared-types"))
    implementation("androidx.core:core-ktx:1.12.0")
}
