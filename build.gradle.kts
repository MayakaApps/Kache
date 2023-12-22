plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false

    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish) apply false

    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
}

subprojects {
    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
}

allprojects {
    repositories {
        mavenCentral()
    }

    afterEvaluate {
        tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
            dokkaSourceSets.configureEach {
                includes.from("module.md")
            }
        }

        tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaTask>().configureEach {
            pluginsMapConfiguration.set(
                mapOf(
                    "org.jetbrains.dokka.base.DokkaBase" to """
                    {
                      "customStyleSheets": [
                        "${rootDir.toString().replace('\\', '/')}/docs/css/kache-dokka.css"
                      ],
                      "customAssets" : [
                        "${rootDir.toString().replace('\\', '/')}/docs/images/kache-logo.png"
                      ],
                      "footerMessage": "Copyright &copy; 2023 MayakaApps."
                    }
                    """.trimIndent()
                )
            )
        }

        extensions.findByType<PublishingExtension>()?.apply {
            val publishApple = when (findProperty("publicationType")) {
                "appleOnly" -> true
                "nonAppleOnly" -> false
                else -> return@apply
            }

            val applePublicationsPrefixes = listOf("macos", "ios", "watchos", "tvos")
            val applePublications = publications.matching { publication ->
                applePublicationsPrefixes.any { publication.name.startsWith(it) }
            }

            tasks.withType<AbstractPublishToMaven>().configureEach {
                onlyIf {
                    if (publishApple) applePublications.any { it == publication }
                    else applePublications.none { it == publication }
                }
            }
        }
    }

    // Workaround for yarn concurrency (issue: https://youtrack.jetbrains.com/issue/KT-43320)
    tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
        args.addAll(listOf("--mutex", "file:${file("build/.yarn-mutex")}"))
    }

    // Workaround for Gradle implicit dependency error on publishing (issue: https://youtrack.jetbrains.com/issue/KT-46466)
    tasks.withType<AbstractPublishToMaven>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>().configureEach {
    moduleName.set("Kache")
    outputDirectory.set(file("$rootDir/docs/api"))
}
