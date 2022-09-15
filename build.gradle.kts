@file:Suppress("SuspiciousCollectionReassignment")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.7.10"
    application
}

group = "io.github.iprodigy.twitch"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = "2.13.4")
    implementation(group = "com.github.twitch4j", name = "twitch4j", version = "1.11.0")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.3.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xuse-k2"
        jvmTarget = "1.8"
    }
}

application {
    mainClass.set("io.github.iprodigy.twitch.MainKt")
}
