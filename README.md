<br />

<div align="center">
    <img src="res/logo.png" alt="Logo"/>
</div>

<h1 align="center" style="margin-top: 0;">Kache</h1>

<div align="center">

![Kache](https://img.shields.io/badge/ache-blue?logo=kotlin)
[![GitHub stars](https://img.shields.io/github/stars/MayakaApps/LruKache)](https://github.com/MayakaApps/LruKache/stargazers)
[![GitHub license](https://img.shields.io/github/license/MayakaApps/LruKache)](https://github.com/MayakaApps/LruKache/blob/main/LICENSE)
![Maven Central](https://img.shields.io/maven-central/v/com.mayakapps.lrucache/lru-cache)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.mayakapps.kache/kache?server=https%3A%2F%2Fs01.oss.sonatype.org)
[![Twitter](https://img.shields.io/twitter/url?style=social&url=https%3A%2F%2Fgithub.com%2FMayakaApps%2FLruKache)](https://twitter.com/intent/tweet?text=Kache%20is%20a%20lightweight%20caching%20library%20for%20Kotlin%20Multiplatform.%20Check%20it%20out.&url=https%3A%2F%2Fgithub.com%2FMayakaApps%2FKache)

</div>

**Kache (previously Kotlinized LRU Cache) is a lightweight Kotlin Multiplatform library for that is inspired by Android's LruCache and [Jake Wharton](https://github.com/JakeWharton)'s DiskLruCache.**

**Note: This README is for the stable v1 version. Currently, v2 that is compatible with Kotlin Multiplatform is a work-in-progress and once it gets stable, I'll update this file and documentation.**

## Setup (Gradle)

Kotlin DSL:

```kotlin
repositories {
    mavenCentral()

    // Add only if you're using snapshot version
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("com.mayakapps.lrucache:lru-cache:<version>")
}
```

Groovy DSL:

```gradle
repositories {
    mavenCentral()
    
    // Add only if you're using snapshot version
    maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots/" }
}

dependencies {
    implementation "com.mayakapps.lrucache:lru-cache:<version>"
}
```

Don't forget to replace `<version>` with the latest/desired version found on the badges above.

## Usage

You can create your cache using `LruCache` constructor or `DiskLruCache.open`, then you can use the usual operators of get, put, and remove and their different implementations.

Sample Code (`LruCache`):

```kotlin
val lruCache = LruCache<String, ByteArray>(
  maxSize = 5 * 1024 * 1024, // 5 MB
)

// ...

val newValue = lruCache.put(uniqueKey) {
    try {
        // Some CPU-intensive process - Returning a not null value means success
    } catch (ex: Throwable) {
        // Handle exception
        null // Caching failed
    }
}

// ...

val cachedValue = lruCache.get(uniqueKey)

```

Sample Code (`DiskLruCache`):

```kotlin
val lruCache = DiskLruCache.open(
  directory = cacheDir,
  maxSize = 10 * 1024 * 1024, // 10 MB
)

// ...

lruCache.getOrPut(imageFilename) { cacheFile ->
    try {
        // downloadFromInternet(imageUrl)
        true // Caching succeeded - Save the file
    } catch (ex: IOException) {
        // Handle exception
        false // Caching failed - Delete the file
    }
}
```

## Documentation

See documentation [here](https://mayakaapps.github.io/LruKache/lrucache/com.mayakapps.lrucache/index.html)

## Why use Kache?

* Coroutine-friendly. Get rid of Java's blocking implementation.
* Much simpler and modern API that helps you do almost whatever you want using a single call.
* (DiskLruCache) Binary journal instead of text-based journal which consumes less storage.

## License

This library is distributed under the MIT license.

## Contributing

All contributions are welcome. If you are reporting an issue, please use the provided template. If you're planning to
contribute to the code, please open an issue first describing what feature you're planning to add or what issue you're
planning to fix. This allows better discussion and coordination of efforts. You can also check open issues for
bugs/features that needs to be fixed/implemented.

## Acknowledgements

These amazing projects have all credit for establishing the algorithms and base implementation for this project.

* Android's [LruCache](https://developer.android.com/reference/android/util/LruCache)
* [Jake Wharton](https://github.com/JakeWharton)'s [DiskLruCache](https://github.com/JakeWharton/DiskLruCache)
