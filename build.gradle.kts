@file:Suppress("SuspiciousCollectionReassignment")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.9.20"
    application
}

group = "io.github.iprodigy.twitch"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = "2.16.0")
    implementation(group = "com.github.twitch4j", name = "twitch4j", version = "1.18.0")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.3.11")
    implementation(platform("io.github.xanthic.cache:cache-bom:0.4.2"))
    implementation(group = "io.github.xanthic.cache", name = "cache-kotlin")
    implementation(group = "io.github.xanthic.cache", name = "cache-provider-caffeine")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xuse-k2"
        jvmTarget = "11"
    }
}

application {
    mainClass.set("io.github.iprodigy.twitch.MainKt")
}
