pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "Kache"
include("collections")
include(":kache")
include(":file-kache-okio")