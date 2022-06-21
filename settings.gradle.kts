pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)

        id("com.vanniktech.maven.publish").version("0.20.0")
    }
}

rootProject.name = "KotlinizedLruCache"
include(":lrucache")
