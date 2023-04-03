pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        kotlin("multiplatform") version "1.8.20"

        id("org.jetbrains.dokka") version "1.8.10"
        id("com.vanniktech.maven.publish") version "0.25.1"
    }
}

rootProject.name = "Kache"
include(":file-kache-okio")
include(":kache")