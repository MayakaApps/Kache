package com.mayakapps.kache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

class JavaFileKache private constructor(
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

    override suspend fun remove(key: String) =
        baseKache.remove(key)

    override suspend fun clear() =
        baseKache.clear()

    override suspend fun close() =
        baseKache.close()

    data class Configuration(
        var directory: File,
        var maxSize: Long,
        var creationScope: CoroutineScope = CoroutineScope(getIODispatcher()),
        var cacheVersion: Int = 1,
        var keyTransformer: KeyTransformer? = SHA256KeyHasher,
    )

    companion object {
        suspend operator fun invoke(
            directory: File,
            maxSize: Long,
            configuration: Configuration.() -> Unit = {},
        ): JavaFileKache {
            val config = Configuration(directory, maxSize).apply(configuration)

            val baseKache = OkioFileKache(
                directory = config.directory.toOkioPath(),
                maxSize = config.maxSize,
            ) {
                creationScope = config.creationScope
                cacheVersion = config.cacheVersion
                keyTransformer = config.keyTransformer
            }

            return JavaFileKache(baseKache, config.creationScope)
        }
    }
}