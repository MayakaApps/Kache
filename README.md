<br />

<!--suppress HtmlDeprecatedAttribute -->
<div align="center">
    <img src="docs/images/kache-logo.png" alt="Logo"/>
</div>

<!--suppress HtmlDeprecatedAttribute -->
<h1 align="center" style="margin-top: 0;">Kache</h1>

<!--suppress HtmlDeprecatedAttribute -->
<div align="center">

![Kache](https://img.shields.io/badge/ache-blue?logo=kotlin)
[![GitHub stars](https://img.shields.io/github/stars/MayakaApps/LruKache)](https://github.com/MayakaApps/LruKache/stargazers)
[![GitHub license](https://img.shields.io/github/license/MayakaApps/LruKache)](https://github.com/MayakaApps/LruKache/blob/main/LICENSE)
![Maven Central](https://img.shields.io/maven-central/v/com.mayakapps.kache/kache)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.mayakapps.kache/kache?server=https%3A%2F%2Fs01.oss.sonatype.org)
[![Twitter](https://img.shields.io/twitter/url?style=social&url=https%3A%2F%2Fgithub.com%2FMayakaApps%2FKache)](https://twitter.com/intent/tweet?text=Kache%20is%20a%20lightweight%20caching%20library%20for%20Kotlin%20Multiplatform.%20Check%20it%20out.&url=https%3A%2F%2Fgithub.com%2FMayakaApps%2FKache)

</div>

Kache is a lightweight Kotlin Multiplatform caching library that supports both
in-memory and persistent caches and supports different eviction strategies (LRU, FIFO, MRU, FILO).

***Supported platforms:***

* **JVM** (and **Android**)
* **JS** (Browser does not support persistent cache)
* **macOS** (macosX64, macosArm64)
* **iOS** (iosArm64, iosX64, iosSimulatorArm64)
* **watchOS** (watchosArm32, watchosArm64, watchosX64, watchosDeviceArm64, watchosSimulatorArm64)
* **tvOS** (tvosArm64, tvosX64, tvosSimulatorArm64)
* **Linux** (linuxX64, linuxArm64)
* **Windows** (mingwX64)
* **androidNative** (androidNativeArm32, androidNativeArm64, androidNativeX86, androidNativeX64): only supported by in-memory cache for now

## Why use Kache?

* **Kotlin Multiplatform.** Use the same code for Android, iOS, and other platforms.
* **In-memory and persistent caches.** Use the same API for both in-memory and persistent caches.
* **Different eviction strategies.** Use any common strategy: LRU, FIFO, MRU, or FILO.
* **Coroutine-friendly.** Get rid of blocking implementations.
* **Simple and modern API** that helps you do almost whatever you want using a single call.

## Setup (Gradle)

Kotlin DSL:

```kotlin
repositories {
    mavenCentral()

    // Add only if you're using snapshot version
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    // For in-memory cache
    implementation("com.mayakapps.kache:kache:<version>")

    // For persistent cache
    implementation("com.mayakapps.kache:file-kache:<version>")
}
```

Groovy DSL:

```groovy
repositories {
    mavenCentral()

    // Add only if you're using snapshot version
    maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots/" }
}

dependencies {
    // For in-memory cache
    implementation "com.mayakapps.kache:kache:<version>"

    // For persistent cache
    implementation "com.mayakapps.kache:file-kache:<version>"
}
```

Don't forget to replace `<version>` with the latest found on the badges above or the desired version.

## Usage

You can create your cache using the following builder DSL, then you can use the usual operators of get, put, and remove
and their different implementations.

Sample Code (`Kache`):

```kotlin
// Could be InMemoryKache
val cache = Kache<String, ByteArray>(maxSize = 5 * 1024 * 1024) {  // 5 MB
    // Other optional configurations
    strategy = KacheStrategy.LRU
    // ...
}

// ...

val newValue = cache.put(uniqueKey) {
    try {
        // Some CPU-intensive process - Returning a not null value means success
    } catch (ex: Throwable) {
        // Handle exception
        null // returning null means creating the value has failed - The value (null) will not be cached
    }
}

// ...

val cachedValue = cache.get(uniqueKey)
```

Sample Code (`FileKache`):

```kotlin
// Could be OkioFileKache or JavaFileKache
val cache = FileKache(directory = "cache", maxSize = 10 * 1024 * 1024) {
    // Other optional configurations
    strategy = KacheStrategy.MRU
    // ...
}

// ...

try {
    val imageData = cache.getOrPut(uniqueKey) { cacheFilename ->
        try {
            // downloadFromInternet(imageUrl, cacheFilename)
            true // returning true means caching has succeeded - The file will be kept
        } catch (ex: IOException) {
            // Handle exception
            false // returning false means caching has failed - The file will be deleted
        }
    }
} finally {
    cache.close()
}
```

## Documentation

See documentation [here](https://mayakaapps.github.io/Kache/latest/)

## License

All the code inside this library is licensed under Apache License 2.0
unless explicitly stated otherwise.

## Contributing

All contributions are welcome. If you are reporting an issue, please use the provided template. If you're planning to
contribute to the code, please open an issue first describing what feature you're planning to add or what issue you're
planning to fix. This allows better discussion and coordination of efforts. You can also check open issues for
bugs/features that needs to be fixed/implemented.
