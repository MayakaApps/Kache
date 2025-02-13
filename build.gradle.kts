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
        dokka {
            pluginsConfiguration.html {
                customStyleSheets.from(rootProject.layout.projectDirectory.file("docs/styles/kache-dokka.css"))
                customAssets.from(rootProject.layout.projectDirectory.file("docs/images/kache-logo.png"))

                // When changing footerMessage, also update it in MkDocs config
                footerMessage.set("Copyright &copy; 2023-2024 MayakaApps.")
            }
        }
    }

    // Workaround for yarn concurrency (issue: https://youtrack.jetbrains.com/issue/KT-43320)
    tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>()
        .configureEach {
            args.addAll(listOf("--mutex", "file:${file("build/.yarn-mutex")}"))
        }

    // Workaround for Gradle implicit dependency error on publishing (issue: https://youtrack.jetbrains.com/issue/KT-46466)
    tasks.withType<AbstractPublishToMaven>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }
}

dokka {
    moduleName.set("Kache")

    dokkaPublications.html {
        outputDirectory.set(layout.projectDirectory.dir("docs/api"))
    }
}
