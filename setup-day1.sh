#!/bin/bash
set -e
cd "$(dirname "$0")"

echo "== Creating root Gradle files =="
cat > settings.gradle.kts <<'S'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "BayeraTravel"
include(":apps:customer-app")
S

cat > gradle.properties <<'S'
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
android.enableJetifier=true
kotlin.code.style=official
S

cat > build.gradle.kts <<'S'
plugins {
    id("com.android.application") version "8.1.0" apply false
    id("com.android.library") version "8.1.0" apply false
    kotlin("android") version "1.9.0" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
S

cat > .gitignore <<'S'
*.iml
.gradle/
local.properties
/.idea/
/build/
/captures/
.externalNativeBuild/
*.keystore
**/google-services.json
.DS_Store
S

echo "== Creating apps/customer-app skeleton =="
mkdir -p apps/customer-app/src/main/java/com/bayera/travel/customer
mkdir -p apps/customer-app/src/main/res/values

cat > apps/customer-app/build.gradle.kts <<'S'
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.bayera.travel.customer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bayera.travel.customer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
}
S

cat > apps/customer-app/src/main/AndroidManifest.xml <<'S'
<?xml version="1.0" encoding="utf-8"?>
<manifest package="com.bayera.travel.customer"
          xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@style/Theme.BayeraTravel">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
S

cat > apps/customer-app/src/main/res/values/strings.xml <<'S'
<resources>
    <string name="app_name">Bayera Travel Customer</string>
</resources>
S

cat > apps/customer-app/src/main/java/com/bayera/travel/customer/MainActivity.kt <<'S'
package com.bayera.travel.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BayeraTravelCustomerApp() }
    }
}

@Composable
fun BayeraTravelCustomerApp() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Greeting("Bayera Customer")
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() { BayeraTravelCustomerApp() }
S

echo "== Files created successfully! =="
