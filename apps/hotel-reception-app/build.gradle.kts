plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.bayera.travel.hotelreception"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.bayera.travel.hotelreception"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}
