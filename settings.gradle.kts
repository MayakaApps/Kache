pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version (extra["kotlin.version"] as String)

        id("org.jetbrains.dokka") version "1.7.0"
        id("com.vanniktech.maven.publish") version "0.20.0"
    }
}

rootProject.name = "KotlinizedLruCache"
include(":lrucache")
