plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

@OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
kotlin {
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

    val appleConfig: org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.() -> Unit = {
        binaries {
            framework {
                baseName = "collections"
            }
        }
    }

    macosX64(appleConfig)
    macosArm64(appleConfig)

    ios(appleConfig)
    iosSimulatorArm64(appleConfig)

    watchos(appleConfig)
    watchosSimulatorArm64(appleConfig)
    watchosDeviceArm64(appleConfig)

    tvos(appleConfig)
    tvosSimulatorArm64(appleConfig)

    linuxX64()
    linuxArm64()

    mingwX64()

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()

    wasm()

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting

        val commonTest by getting

        val nativeMain by creating {
            dependsOn(commonMain)
        }

        val nativeTest by creating {
            dependsOn(commonTest)

            dependsOn(nativeMain)
        }

        val jvmMain by getting

        val jvmTest by getting

        val jsMain by getting

        val jsTest by getting

        val appleMain by creating {
            dependsOn(nativeMain)
        }

        val appleTest by creating {
            dependsOn(nativeTest)

            dependsOn(appleMain)
        }

        val macosX64Main by getting
        val macosArm64Main by getting
        val macosMain by creating {
            dependsOn(appleMain)

            // There is no built-in macOS target shortcut
            macosX64Main.dependsOn(this)
            macosArm64Main.dependsOn(this)
        }

        val macosX64Test by getting
        val macosArm64Test by getting
        val macosTest by creating {
            dependsOn(appleTest)

            dependsOn(macosMain)
            macosX64Test.dependsOn(this)
            macosArm64Test.dependsOn(this)
        }

        val iosSimulatorArm64Main by getting
        val iosMain by getting {
            dependsOn(appleMain)

            // iOS target shortcut only contains: iosArm64, iosX64
            iosSimulatorArm64Main.dependsOn(this)
        }

        val iosSimulatorArm64Test by getting
        val iosTest by getting {
            dependsOn(appleTest)

            iosSimulatorArm64Test.dependsOn(this)
        }

        val watchosSimulatorArm64Main by getting
        val watchosDeviceArm64Main by getting
        val watchosMain by getting {
            dependsOn(appleMain)

            // watchOS target shortcut only contains: watchosArm32, watchosArm64, watchosX64
            watchosSimulatorArm64Main.dependsOn(this)
            watchosDeviceArm64Main.dependsOn(this)
        }

        val watchosSimulatorArm64Test by getting
        val watchosDeviceArm64Test by getting
        val watchosTest by getting {
            dependsOn(appleTest)

            watchosSimulatorArm64Test.dependsOn(this)
            watchosDeviceArm64Test.dependsOn(this)
        }

        val tvosSimulatorArm64Main by getting
        val tvosMain by getting {
            dependsOn(appleMain)

            // tvOS target shortcut only contains: tvosArm64, tvosX64
            tvosSimulatorArm64Main.dependsOn(this)
        }

        val tvosSimulatorArm64Test by getting
        val tvosTest by getting {
            dependsOn(appleTest)

            tvosSimulatorArm64Test.dependsOn(this)
        }

        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }

        val linuxX64Test by getting {
            dependsOn(nativeTest)
        }

        val linuxArm64Main by getting {
            dependsOn(nativeMain)
        }

        val linuxArm64Test by getting {
            dependsOn(nativeTest)
        }

        val mingwX64Main by getting {
            dependsOn(nativeMain)
        }

        val mingwX64Test by getting {
            dependsOn(nativeTest)
        }

        val androidNativeMain by creating {
            dependsOn(nativeMain)
        }

        val androidNativeTest by creating {
            dependsOn(nativeTest)

            dependsOn(androidNativeMain)
        }

        val androidNativeArm32Main by getting {
            dependsOn(androidNativeMain)
        }

        val androidNativeArm32Test by getting {
            dependsOn(androidNativeTest)
        }

        val androidNativeArm64Main by getting {
            dependsOn(androidNativeMain)
        }

        val androidNativeArm64Test by getting {
            dependsOn(androidNativeTest)
        }

        val androidNativeX86Main by getting {
            dependsOn(androidNativeMain)
        }

        val androidNativeX86Test by getting {
            dependsOn(androidNativeTest)
        }

        val androidNativeX64Main by getting {
            dependsOn(androidNativeMain)
        }

        val androidNativeX64Test by getting {
            dependsOn(androidNativeTest)
        }

        val wasmMain by getting {
            dependsOn(nativeMain)
        }

        val wasmTest by getting {
            dependsOn(nativeTest)
        }
    }
}