plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    kotlin("jvm") apply false
}

subprojects {
    repositories {
        mavenCentral()
    }
}