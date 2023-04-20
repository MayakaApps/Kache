plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

@OptIn(
    org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class,
    org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class,
)
kotlin {
    targetHierarchy.default()

    jvm {
        compilations.configureEach {
            kotlinOptions.jvmTarget = "1.8"
        }

        withJava()
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

    wasm() {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        nodejs()

        d8()
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting

        val nativeAndWasmMain by creating {
            dependsOn(commonMain)
        }

        val nativeMain by getting {
            dependsOn(nativeAndWasmMain)
        }

        val wasmMain by getting {
            dependsOn(nativeAndWasmMain)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}