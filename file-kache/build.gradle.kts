import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        nodejs()
    }

    val appleConfig: KotlinNativeTarget.() -> Unit = {
        binaries {
            framework {
                baseName = "file-kache"
            }
        }
    }

    macosX64(appleConfig)
    macosArm64(appleConfig)

    iosArm64(appleConfig)
    iosX64(appleConfig)
    iosSimulatorArm64(appleConfig)

    watchosArm32(appleConfig)
    watchosArm64(appleConfig)
    watchosX64(appleConfig)
    watchosSimulatorArm64(appleConfig)
    // Not supported by dependencies
    // watchosDeviceArm64(appleConfig)

    tvosArm64(appleConfig)
    tvosX64(appleConfig)
    tvosSimulatorArm64(appleConfig)

    linuxX64()
    // Not supported by dependencies
    // linuxArm64()

    mingwX64()

    // Not supported by dependencies
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
                implementation(project(":file-kache-base"))
                api(project(":file-kache-utils"))

                implementation(libs.kotlinx.coroutines.core)

                implementation(libs.okio)
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