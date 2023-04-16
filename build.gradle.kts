plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false

    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.mavenPublish) apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}