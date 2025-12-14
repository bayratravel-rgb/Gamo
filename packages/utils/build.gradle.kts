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
    implementation(project(":packages:shared-types"))
}
