pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "Kache"
include("collections")
include(":kache")
include(":file-kache-base")
include(":file-kache-common")
include(":file-kache")
include(":file-kache-okio")