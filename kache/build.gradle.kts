plugins {
    alias(libs.plugins.kotlin.multiplatform)

    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    jvm {
        compilations.configureEach {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    fun org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl.configureTests() {
        testTask {
            useMocha {
                timeout = "30s"
            }
        }
    }

    js {
        browser { configureTests() }
        nodejs { configureTests() }
    }

    // Still experimental
    // Blocked by coroutines (issue: https://github.com/Kotlin/kotlinx.coroutines/issues/3713), Stately, and Kotest (depends on coroutines)
    // wasmJs()
    // wasmWasi()

    macosX64()
    macosArm64()

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    watchosArm32()
    watchosArm64()
    watchosX64()
    // Blocked by Kotest (expected in 5.9.0)
    // watchosDeviceArm64()
    watchosSimulatorArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    linuxX64()
    linuxArm64()

    mingwX64()

    // Blocked by Kotest (expected in 5.9.0)
    // androidNativeArm32()
    // androidNativeArm64()
    // androidNativeX86()
    // androidNativeX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        configureEach {
            languageSettings.optIn("kotlin.RequiresOptIn")
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

                implementation(libs.kotest.assertions)

                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val nonJvmMain by creating {
            dependsOn(commonMain.get())

            dependencies {
                implementation(libs.stately.isoCollections)
            }
        }

        nativeMain.get().dependsOn(nonJvmMain)
        jsMain.get().dependsOn(nonJvmMain)
    }
}
