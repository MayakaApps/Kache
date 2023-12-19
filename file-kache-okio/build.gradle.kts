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
        // JS browser target has no FileSystem implementation in Okio
        // browser { configureTests() }

        nodejs { configureTests() }
    }

    // Still experimental
    // Blocked by Okio (issue: https://github.com/square/okio/issues/1203)
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
    // Blocked by Okio (issue: https://github.com/square/okio/issues/1242)
    // watchosDeviceArm64()
    watchosSimulatorArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    linuxX64()
    linuxArm64()

    mingwX64()

    // Blocked by Okio (issue: https://github.com/square/okio/issues/1242)
    // androidNativeArm32()
    // androidNativeArm64()
    // androidNativeX86()
    // androidNativeX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":file-kache-okio-only"))
                api(project(":file-kache"))

                api(libs.okio)
            }
        }

        jsMain {
            dependencies {
                implementation(libs.okio.nodeFilesystem)
            }
        }
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
    dokkaSourceSets.named("commonMain") {
        skipEmptyPackages.set(false)
    }
}
