plugins {
    id("com.android.application") version "8.2.1" apply false
    id("com.android.library") version "8.2.1" apply false
    kotlin("android") version "1.9.0" apply false
}
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
