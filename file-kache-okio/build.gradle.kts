plugins {
    alias(libs.plugins.kotlin.multiplatform)

    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    targetHierarchy.default()

    jvm {
        compilations.configureEach {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    js(IR) {
        // Has no FileSystem implementation in Okio
        // browser()

        nodejs()
    }

    // Still experimental
    // Blocked by Okio (issue: https://github.com/square/okio/issues/1203)
    // wasm()

    macosX64()
    macosArm64()

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    watchosArm32()
    watchosArm64()
    watchosX64()
    // Blocked by Okio (issue: https://github.com/square/okio/issues/1242)
    // watchosDeviceArm64()
    watchosSimulatorArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    linuxX64()
    // Blocked by Okio (issue: https://github.com/square/okio/issues/1242)
    // linuxArm64()

    mingwX64()

    // Blocked by Okio (issue: https://github.com/square/okio/issues/1242)
    // androidNativeArm32()
    // androidNativeArm64()
    // androidNativeX86()
    // androidNativeX64()

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":file-kache-base"))
                api(project(":file-kache"))
                api(project(":file-kache-common"))

                api(libs.okio)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.okio.nodeFilesystem)
            }
        }
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
        sourceRoot(file("../kache-common/src/$name"))
        sourceRoot(file("../file-kache-common/src/$name"))
        sourceRoot(file("../file-kache-base/src/$name"))
        sourceRoot(file("../file-kache/src/$name"))
    }
}