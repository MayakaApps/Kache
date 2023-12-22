pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "Kache"
include(":kache")
include(":file-kache-core")
include(":file-kache-okio-only")
include(":file-kache")
include(":file-kache-okio")
