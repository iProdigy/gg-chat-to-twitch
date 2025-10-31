import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.gradleup.shadow") version "8.3.9"
    kotlin("jvm") version "2.2.21"
    application
}

group = "io.github.iprodigy.twitch"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = "2.20.1")
    implementation(group = "com.github.twitch4j", name = "twitch4j", version = "1.25.0")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.5.20")
    implementation(platform("io.github.xanthic.cache:cache-bom:0.7.1"))
    implementation(group = "io.github.xanthic.cache", name = "cache-kotlin")
    implementation(group = "io.github.xanthic.cache", name = "cache-jackson")
    implementation(group = "io.github.xanthic.cache", name = "cache-provider-caffeine")
}

tasks.compileJava {
    options.release = 11
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

application {
    mainClass.set("io.github.iprodigy.twitch.MainKt")
}
