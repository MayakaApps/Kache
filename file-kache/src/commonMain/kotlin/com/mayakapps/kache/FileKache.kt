package com.mayakapps.kache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import okio.Path
import okio.Path.Companion.toPath

class FileKache private constructor(
    private val baseKache: ContainerKache<String, Path>,
    private val creationScope: CoroutineScope,
) : ContainerKache<String, String> {

    override suspend fun get(key: String): String? =
        baseKache.get(key)?.toString()

    override suspend fun getIfAvailable(key: String): String? =
        baseKache.getIfAvailable(key)?.toString()

    override suspend fun getOrPut(key: String, creationFunction: suspend (String) -> Boolean): String? =
        baseKache.getOrPut(key) { file ->
            creationFunction(file.toString())
        }?.toString()

    override suspend fun put(key: String, creationFunction: suspend (String) -> Boolean): String? =
        baseKache.put(key) { file ->
            creationFunction(file.toString())
        }?.toString()

    override suspend fun putAsync(key: String, creationFunction: suspend (String) -> Boolean): Deferred<String?> =
        creationScope.async {
            baseKache.putAsync(key) { file ->
                creationFunction(file.toString())
            }.await()?.toString()
        }

    override suspend fun remove(key: String) =
        baseKache.remove(key)

    override suspend fun clear() =
        baseKache.clear()

    override suspend fun close() =
        baseKache.close()

    data class Configuration(
        var directoryPath: String,
        var maxSize: Long,
        var strategy: KacheStrategy = KacheStrategy.LRU,
        var creationScope: CoroutineScope = CoroutineScope(ioDispatcher),
        var cacheVersion: Int = 1,
        var keyTransformer: KeyTransformer? = SHA256KeyHasher,
    )

    companion object {
        suspend operator fun invoke(
            directoryPath: String,
            maxSize: Long,
            configuration: Configuration.() -> Unit = {},
        ): FileKache {
            val config = Configuration(directoryPath, maxSize).apply(configuration)

            val baseKache = OkioFileKache(
                directory = config.directoryPath.toPath(),
                maxSize = config.maxSize,
            ) {
                strategy = config.strategy
                creationScope = config.creationScope
                cacheVersion = config.cacheVersion
                keyTransformer = config.keyTransformer
            }

            return FileKache(baseKache, config.creationScope)
        }
    }
}