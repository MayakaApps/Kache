plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    kotlin("multiplatform") apply false

    id("org.jetbrains.dokka") apply false
    id("com.vanniktech.maven.publish") apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}