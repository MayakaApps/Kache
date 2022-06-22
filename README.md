<br />

<div align="center">
    <img src="res/logo.png" alt="Logo"/>
</div>

<h1 align="center" style="margin-top: 0;">Kotlinized LRU Cache</h1>

<div align="center">

![Kotlinized LRU Cache](https://img.shields.io/badge/Kotlinized-LRU%20Cache-blue?logo=kotlin)
[![GitHub stars](https://img.shields.io/github/stars/MayakaApps/KotlinizedLruCache)](https://github.com/MayakaApps/KotlinizedLruCache/stargazers)
[![GitHub license](https://img.shields.io/github/license/MayakaApps/KotlinizedLruCache)](https://github.com/MayakaApps/KotlinizedLruCache/blob/main/LICENSE)
![Maven Central](https://img.shields.io/maven-central/v/com.mayakapps.lrucache/lru-cache)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.mayakapps.lrucache/lru-cache?server=https%3A%2F%2Fs01.oss.sonatype.org)
[![Twitter](https://img.shields.io/twitter/url?style=social&url=https%3A%2F%2Fgithub.com%2FMayakaApps%2FKotlinizedLruCache)](https://twitter.com/intent/tweet?text=Kotlinized%20LRU%20Cache%20is%20a%20lightweight%20library%20for%20Kotlin%2FJVM%20that%20is%20based%20on%20Android%27s%20LruCache%20and%20Jake%20Wharton%27s%20DiskLruCache.&url=https%3A%2F%2Fgithub.com%2FMayakaApps%2FKotlinizedLruCache)

</div>

**Kotlinized LRU Cache is a lightweight library for Kotlin/JVM that is based on Android's LruCache and [Jake Wharton](https://github.com/JakeWharton)'s DiskLruCache.**

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

Sample Code:

```kotlin
val lruCache = DiskLruCache.open(
  directory = cacheDir,
  maxSize = 10 * 1024 * 1024, // 10 MB
)

// ...

lruCache.getOrPut(imageFilename) { /* Download from network */ }
```

## Documentation

See documentation [here](https://mayakaapps.github.io/KotlinizedLruCache/lrucache/com.mayakapps.lrucache/index.html)

## Benefits over acknowledged projects

* New implementation which is coroutine-safe - not thread-blocking
* Much simpler and modern API that is helps you do almost whatever you want using a single call.
* (DiskLruCache) Binary journal instead of text-based journal which provides minor storage space.

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
