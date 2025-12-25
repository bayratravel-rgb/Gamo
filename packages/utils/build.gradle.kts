plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bayera.travel.utils"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
}

dependencies {
    api(project(":packages:shared-types")) // API = Visible to apps using Utils
    implementation("androidx.core:core-ktx:1.12.0")
}
