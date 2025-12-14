plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.bayera.travel.data"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
}
dependencies {
    implementation(project(":packages:shared-types"))
    implementation(project(":packages:utils"))
}
