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

    val appleConfig: org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.() -> Unit = {
        binaries {
            framework {
                baseName = "file-kache-okio"
            }
        }
    }

    macosX64(appleConfig)
    macosArm64(appleConfig)

    ios(appleConfig)
    iosSimulatorArm64(appleConfig)

    watchos(appleConfig)
    watchosSimulatorArm64(appleConfig)
    // Not supported by Coroutines
    // Issue: https://github.com/Kotlin/kotlinx.coroutines/issues/3601
    // watchosDeviceArm64(appleConfig)

    tvos(appleConfig)
    tvosSimulatorArm64(appleConfig)

    linuxX64()
    // Not supported by Coroutines
    // Issue: https://github.com/Kotlin/kotlinx.coroutines/issues/855
    // linuxArm64()

    mingwX64()

    // Not supported by Coroutines
    // Issue: https://github.com/Kotlin/kotlinx.coroutines/issues/812
    // androidNativeArm32()
    // androidNativeArm64()
    // androidNativeX86()
    // androidNativeX64()

    // Still experimental and not supported by dependencies
    // wasm()

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":file-kache-base"))
                api(project(":file-kache"))
                api(project(":file-kache-utils"))

                api(libs.okio)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.okio.nodeFilesystem)
            }
        }
    }

    val publicationsFromMainHost =
        listOf(jvm(), js(), linuxX64(), mingwX64()).map { it.name } + "kotlinMultiplatform"
    publishing {
        publications {
            matching { it.name in publicationsFromMainHost }.all {
                val targetPublication = this@all
                tasks.withType<AbstractPublishToMaven>()
                    .matching { it.publication == targetPublication }
                    .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
            }
        }
    }
}