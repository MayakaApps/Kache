import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")

    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }

        withJava()
    }

    val appleConfig: KotlinNativeTarget.() -> Unit = {
        binaries {
            framework {
                baseName = "lru-cache"
            }
        }
    }

    macosX64(appleConfig)
    macosArm64(appleConfig)

    ios(appleConfig)
    iosArm32(appleConfig)
    iosSimulatorArm64(appleConfig)

    watchos(appleConfig)
    watchosX86(appleConfig)
    watchosSimulatorArm64(appleConfig)

    tvos(appleConfig)
    tvosSimulatorArm64(appleConfig)

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-assertions-core:5.5.4")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
            }
        }

        val appleMain by creating {
            dependsOn(commonMain)

            dependencies {
                implementation("co.touchlab:stately-iso-collections:1.2.0")
            }
        }

        val appleTest by creating {
            dependsOn(commonTest)
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

        val iosArm32Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by getting {
            dependsOn(appleMain)

            // iOS target shortcut only contains: iosArm64, iosX64
            iosArm32Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }

        val iosArm32Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by getting {
            dependsOn(appleTest)

            iosArm32Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }

        val watchosX86Main by getting
        val watchosSimulatorArm64Main by getting
        val watchosMain by getting {
            dependsOn(appleMain)

            // watchOS target shortcut only contains: watchosArm32, watchosArm64, watchosX64
            watchosX86Main.dependsOn(this)
            watchosSimulatorArm64Main.dependsOn(this)
        }

        val watchosX86Test by getting
        val watchosSimulatorArm64Test by getting
        val watchosTest by getting {
            dependsOn(appleTest)

            watchosX86Test.dependsOn(this)
            watchosSimulatorArm64Test.dependsOn(this)
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
    }
}