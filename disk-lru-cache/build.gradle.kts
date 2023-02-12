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
                baseName = "disk-lru-cache"
            }
        }
    }

    macosX64(appleConfig)
    macosArm64(appleConfig)

    ios(appleConfig)
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
                implementation(project(":lru-cache"))

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

        val appleMain by creating {
            dependsOn(commonMain)
        }

        val appleTest by creating {
            dependsOn(commonTest)
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
        val watchosMain by getting {
            dependsOn(appleMain)

            // watchOS target shortcut only contains: watchosArm32, watchosArm64, watchosX64
            watchosSimulatorArm64Main.dependsOn(this)
        }

        val watchosSimulatorArm64Test by getting
        val watchosTest by getting {
            dependsOn(appleTest)

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