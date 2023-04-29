Kache (previously Kotlinized LRU Cache) is a lightweight Kotlin Multiplatform caching library that supports both
in-memory and persistent caches and supports different eviction strategies (LRU, FIFO, MRU, FILO).

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
    implementation("com.mayakapps.kache:kache:{{ versions.kache }}")

    // For persistent cache (in non-Okio projects)
    implementation("com.mayakapps.kache:file-kache:{{ versions.kache }}")

    // For persistent cache (in Okio projects)
    implementation("com.mayakapps.kache:file-kache-okio:{{ versions.kache }}")
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
    implementation "com.mayakapps.kache:kache:{{ versions.kache }}"

    // For persistent cache (in non-Okio projects)
    implementation "com.mayakapps.kache:file-kache:{{ versions.kache }}"

    // For persistent cache (in Okio projects)
    implementation "com.mayakapps.kache:file-kache-okio:{{ versions.kache }}"
}
```

## Usage

You can create your cache using the following builder DSL, then you can use the usual operators of get, put, and remove
and their different implementations.

Sample Code (`Kache`):

```kotlin
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
val cache = FileKache(directoryPath = "cache", maxSize = 10 * 1024 * 1024) {
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

## License

This library is distributed under the MIT license. All the code inside this library is licensed under the MIT license
except for the code inside the module `:collections` which is licensed under the Apache 2.0 license or GPL 2.0 with
classpath exception.

## Contributing

All contributions are welcome. If you are reporting an issue, please use the provided template. If you're planning to
contribute to the code, please open an issue first describing what feature you're planning to add or what issue you're
planning to fix. This allows better discussion and coordination of efforts. You can also check open issues for
bugs/features that needs to be fixed/implemented.

## Acknowledgements

These amazing projects have all credit for establishing the algorithms and base implementation for this project.

* Android's [LruCache](https://developer.android.com/reference/android/util/LruCache)
* [Jake Wharton](https://github.com/JakeWharton)'s [DiskLruCache](https://github.com/JakeWharton/DiskLruCache)