plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.bayera.travel.delivery"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.bayera.travel.delivery"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}
