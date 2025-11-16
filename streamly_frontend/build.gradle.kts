/**
 * Top-level build file
 * Note: Gradle wrapper (8.7) and AGP (8.3.0) are compatible. Compose compiler extension 1.5.8
 * is compatible with Kotlin 1.9.22. No version changes required for packaging stability.
 */
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.3.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

plugins {
    id("com.android.application") version "8.3.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
