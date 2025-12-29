plugins {
    application
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("com.bayera.travel.backend.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-gson:2.3.7")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    // ADDED: Missing JSON library
    implementation("org.json:json:20231013")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks {
    shadowJar {
        archiveBaseName.set("ktor-server")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}
