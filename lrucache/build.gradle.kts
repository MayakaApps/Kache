plugins {
    id("java-library")
    id("kotlin")

    id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
}

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").java.srcDirs)
}

artifacts {
    archives(sourcesJar)
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                setUrl("https://maven.pkg.github.com/Mayaka-Apps/Kotlin-LruCache")
            }
        }

        publications {
            create<MavenPublication>("library") {
                groupId = "com.mayakapps.lrucache"
                artifactId = "lrucache"
                version = "1.0.0"

                from(components["java"])
                artifact(sourcesJar)
            }
        }
    }
}