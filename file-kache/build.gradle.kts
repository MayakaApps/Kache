import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)

    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    explicitApi()

    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions.jvmTarget = JvmTarget.JVM_1_8
    }

    js {
        // JS browser target has no FileSystem implementation in Okio
        // browser()
        nodejs()
    }

    // Still experimental - blocked by coroutines
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
    watchosDeviceArm64()
    watchosSimulatorArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    linuxX64()
    linuxArm64()

    mingwX64()

    // Blocked by Okio
    // androidNativeArm32()
    // androidNativeArm64()
    // androidNativeX86()
    // androidNativeX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":kache"))
                api(libs.okio)

                implementation(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))

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
