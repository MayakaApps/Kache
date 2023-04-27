plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false

    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish) apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    afterEvaluate {
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
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>().configureEach {
    outputDirectory.set(file("$rootDir/docs/api"))

    moduleName.set("Kache")
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """
                {
                    "footerMessage": "Copyright &copy; 2023 MayakaApps."
                }
            """
        )
    )
}