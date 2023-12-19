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
        // Has no FileSystem implementation in Okio
        // browser { configureTests() }

        nodejs { configureTests() }
    }

    // Still experimental
    // Blocked by coroutines (issue: https://github.com/Kotlin/kotlinx.coroutines/issues/3713),
    // Okio (issue: https://github.com/square/okio/issues/1203), and Kotest (depends on coroutines)
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
    // Blocked by coroutines (expected in 1.7.0), Okio (issue: https://github.com/square/okio/issues/1242), and Kotest (depends on coroutines)
    // watchosDeviceArm64()
    watchosSimulatorArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    linuxX64()
    // Blocked by coroutines (expected in 1.7.0), Okio (issue: https://github.com/square/okio/issues/1242), and Kotest (depends on coroutines)
    // linuxArm64()

    mingwX64()

    // Blocked by coroutines (expected in 1.7.0), Okio (issue: https://github.com/square/okio/issues/1242), and Kotest (depends on coroutines)
    // androidNativeArm32()
    // androidNativeArm64()
    // androidNativeX86()
    // androidNativeX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kache"))
                api(project(":file-kache-core"))

                implementation(libs.kotlinx.coroutines.core)

                implementation(libs.okio)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))

                implementation(libs.kotest.assertions)

                implementation(libs.kotlinx.coroutines.test)

                implementation(libs.okio.fakeFilesystem)
            }
        }

        jsMain {
            dependencies {
                implementation(libs.okio.nodeFilesystem)
            }
        }
    }
}