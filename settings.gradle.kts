pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)
    }
}

rootProject.name = "LruCache"
include(":lrucache")
