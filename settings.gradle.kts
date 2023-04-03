pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        kotlin("multiplatform") version "1.8.20"

        id("org.jetbrains.dokka") version "1.7.20"
        id("com.vanniktech.maven.publish") version "0.25.1"
    }
}

rootProject.name = "KotlinLruCache"
include(":lru-cache")
include(":disk-lru-cache")