package com.mayakapps.kache

import com.mayakapps.kache.JavaFileKache.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * A persistent coroutine-safe [ContainerKache] implementation that uses [Okio](https://square.github.io/okio/) to
 * store files under the hood and exposes a Java's File-based API.
 *
 * It uses a journal file to keep track of the cache state and to ensure that the cache is always in a consistent state.
 *
 * It can be built using the following syntax:
 * ```
 * val cache = OkioFileKache(directory = File("cache"), maxSize = 100L * 1024L * 1024L) {
 *     strategy = KacheStrategy.LRU
 *     // ...
 * }
 * ```
 *
 * @see Configuration
 */
public class JavaFileKache internal constructor(
    private val baseKache: ContainerKache<String, Path>,
    private val creationScope: CoroutineScope,
) : ContainerKache<String, File> {

    override suspend fun get(key: String): File? =
        baseKache.get(key)?.toFile()

    override suspend fun getIfAvailable(key: String): File? =
        baseKache.getIfAvailable(key)?.toFile()

    override suspend fun getOrPut(key: String, creationFunction: suspend (File) -> Boolean): File? =
        baseKache.getOrPut(key) { file ->
            creationFunction(file.toFile())
        }?.toFile()

    override suspend fun put(key: String, creationFunction: suspend (File) -> Boolean): File? =
        baseKache.put(key) { file ->
            creationFunction(file.toFile())
        }?.toFile()

    override suspend fun putAsync(key: String, creationFunction: suspend (File) -> Boolean): Deferred<File?> =
        creationScope.async {
            baseKache.putAsync(key) { file ->
                creationFunction(file.toFile())
            }.await()?.toFile()
        }

    override suspend fun remove(key: String): Unit =
        baseKache.remove(key)

    override suspend fun clear(): Unit =
        baseKache.clear()

    override suspend fun close(): Unit =
        baseKache.close()

    /**
     * Configuration for [JavaFileKache]. It is used as a receiver of [JavaFileKache] builder which is [invoke].
     */
    public class Configuration(
        /**
         * The directory where the cache files and the journal will be stored.
         */
        public var directory: File,

        /**
         * The maximum size of the cache in bytes.
         */
        public var maxSize: Long,

        /**
         * The strategy used to evict entries from the cache.
         */
        public var strategy: KacheStrategy = KacheStrategy.LRU,

        /**
         * The coroutine dispatcher used for executing `creationFunction` of put requests.
         */
        public var creationScope: CoroutineScope = CoroutineScope(getIODispatcher()),

        /**
         * The version of the cache. This is useful to invalidate the cache when the format of the data stored in the
         * cache changes.
         */
        public var cacheVersion: Int = 1,

        /**
         * The [KeyTransformer] used to transform the keys before they are used to store and retrieve data. It is
         * needed to avoid using invalid characters in the file names.
         */
        public var keyTransformer: KeyTransformer? = SHA256KeyHasher,
    )
}

/**
 * Creates a new [JavaFileKache] instance with the given [directory] and [maxSize] and is configured by
 * [configuration].
 *
 * @see JavaFileKache.Configuration
 */
public suspend fun JavaFileKache(
    directory: File,
    maxSize: Long,
    configuration: Configuration.() -> Unit = {},
): JavaFileKache {
    val config = Configuration(directory, maxSize).apply(configuration)

    val baseKache = OkioFileKache(
        directory = config.directory.toOkioPath(),
        maxSize = config.maxSize,
    ) {
        strategy = config.strategy
        creationScope = config.creationScope
        cacheVersion = config.cacheVersion
        keyTransformer = config.keyTransformer
    }

    return JavaFileKache(baseKache, config.creationScope)
}
