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

tasks.create("getVersion") {
    doLast {
        println(findProperty("VERSION_NAME"))
    }
}