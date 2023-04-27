plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    targetHierarchy.default()

    jvm {
        compilations.configureEach {
            kotlinOptions.jvmTarget = "1.8"
        }

        withJava()
    }

    js(IR) {
        browser()
        nodejs()
    }

    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasm {
        browser()
        nodejs()
        d8()
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