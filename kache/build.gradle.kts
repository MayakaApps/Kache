@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)

    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    explicitApi()

    jvm {
        compilerOptions.jvmTarget = JvmTarget.JVM_1_8
    }

    js {
        browser()

        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
    }

    wasmJs {
        browser()
        nodejs()
    }

    wasmWasi {
        nodejs()
    }

    macosX64()
    macosArm64()

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosDeviceArm64()
    watchosSimulatorArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    linuxX64()
    linuxArm64()

    mingwX64()

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        configureEach {
            languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
        }

        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))

                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

dokka {
    dokkaSourceSets.commonMain {
        includes.from("module.md")
    }
}
