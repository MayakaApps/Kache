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

    macosX64()
    macosArm64()

    ios()
    iosSimulatorArm64()

    watchos()
    watchosSimulatorArm64()
    // Not supported by Coroutines
    // Issue: https://github.com/Kotlin/kotlinx.coroutines/issues/3601
    // watchosDeviceArm64()

    tvos()
    tvosSimulatorArm64()

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
                implementation(project(":collections"))

                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))

                implementation(libs.kotest.assertions)

                implementation(libs.kotlinx.coroutines.test)
            }
        }


        val nonJvmMain by creating {
            dependsOn(commonMain)

            dependencies {
                implementation(libs.stately.isoCollections)
            }
        }

        val nativeMain by getting {
            dependsOn(nonJvmMain)
        }

        val jsMain by getting {
            dependsOn(nonJvmMain)
        }
    }
}