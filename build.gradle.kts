plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false

    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish) apply false
}

allprojects {
    repositories {
        mavenCentral()
    }

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
    moduleName.set("Kache")
    outputDirectory.set(file("$rootDir/docs/api"))
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