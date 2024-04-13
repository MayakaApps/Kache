/*
 * Copyright 2023-2024 MayakaApps
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mayakapps.kache

import com.mayakapps.kache.JavaFileKache.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
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
 * val cache = JavaFileKache(directory = File("cache"), maxSize = 100L * 1024L * 1024L) {
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

    /**
     * Returns the maximum capacity of this cache in bytes.
     *
     * This does not include the size of the journal.
     */
    override val maxSize: Long get() = baseKache.maxSize

    /**
     * Returns the current size of the cache in bytes.
     *
     * This does not include the size of the journal.
     */
    override val size: Long get() = baseKache.size

    override suspend fun getKeys(): Set<String> = baseKache.getKeys()

    override suspend fun getUnderCreationKeys(): Set<String> = baseKache.getUnderCreationKeys()

    override suspend fun getAllKeys(): KacheKeys<String> = baseKache.getAllKeys()

    /**
     * Returns the file corresponding to the given [key] if it exists or is currently being created, or `null` otherwise.
     *
     * The function waits for the creation to complete if it is in progress. If the creation fails, the function returns
     * `null`. Any unhandled exceptions inside the creation block will be thrown.
     */
    override suspend fun get(key: String): File? = baseKache.get(key)?.toFile()

    /**
     * Returns the file corresponding to the given [key], or `null` if such a key is not present in the cache.
     *
     * This function does not wait for the creation of the file if it is in progress, returning `null` instead.
     */
    override suspend fun getIfAvailable(key: String): File? = baseKache.getIfAvailable(key)?.toFile()

    /**
     * Returns the file corresponding to the given [key] if it exists or is currently being created, a new file
     * serialized by [creationFunction], or `null` if the creation fails.
     *
     * The function waits for the creation to complete if it is in progress. If the creation fails, the function returns
     * `null`. Any unhandled exceptions inside the creation block will be thrown. [creationFunction] is NOT used as a
     * fallback if the current creation fails.
     *
     * [creationFunction] should return `true` if the creation was successful, and `false` otherwise.
     */
    override suspend fun getOrPut(key: String, creationFunction: suspend (File) -> Boolean): File? =
        baseKache.getOrPut(key) { file ->
            creationFunction(file.toFile())
        }?.toFile()

    /**
     * Associates a new file serialized by [creationFunction] with the given [key].
     *
     * This function waits for the creation to complete. If the creation fails, the function returns `null`. Any
     * unhandled exceptions inside the creation block will be thrown. Existing or under-creation files associated
     * with [key] will be replaced by the new file.
     *
     * [creationFunction] should return `true` if the creation was successful, and `false` otherwise.
     */
    override suspend fun put(key: String, creationFunction: suspend (File) -> Boolean): File? =
        baseKache.put(key) { file ->
            creationFunction(file.toFile())
        }?.toFile()

    /**
     * Associates a new file serialized by [creationFunction] asynchronously with the given [key].
     *
     * Any unhandled exceptions inside the creation block will be thrown. Existing or under-creation files associated
     * with [key] will be replaced by the new file.
     *
     * [creationFunction] should return `true` if the creation was successful, and `false` otherwise.
     *
     * Returns: a [Deferred] that will complete with the new file if the creation was successful, or `null` otherwise.
     */
    override suspend fun putAsync(key: String, creationFunction: suspend (File) -> Boolean): Deferred<File?> =
        creationScope.async(start = CoroutineStart.UNDISPATCHED) {
            baseKache.putAsync(key) { file ->
                creationFunction(file.toFile())
            }.await()?.toFile()
        }

    /**
     * Removes the specified [key] and its corresponding file from the cache.
     *
     * If the file is under creation, the creation will be cancelled.
     */
    override suspend fun remove(key: String) {
        baseKache.remove(key)
    }

    /**
     * Removes all keys and their corresponding files from the cache.
     */
    override suspend fun clear() {
        baseKache.clear()
    }

    override suspend fun removeAllUnderCreation() {
        baseKache.removeAllUnderCreation()
    }

    override suspend fun resize(maxSize: Long) {
        baseKache.resize(maxSize)
    }

    override suspend fun trimToSize(size: Long) {
        baseKache.trimToSize(size)
    }

    override suspend fun close() {
        baseKache.close()
    }

    /**
     * Configuration for [JavaFileKache] used as a receiver for its builder function.
     */
    public class Configuration(
        /**
         * The directory where the cache files and the journal will be stored.
         */
        public var directory: File,

        /**
         * The maximum capacity of the cache.
         */
        public var maxSize: Long,
    ) {

        /**
         * The strategy used for evicting elements.
         *
         * @see KacheStrategy
         */
        public var strategy: KacheStrategy = KacheStrategy.LRU

        /**
         * The coroutine dispatcher used for executing `creationFunction` of put requests.
         */
        public var creationScope: CoroutineScope = CoroutineScope(FileKacheDefaults.defaultCoroutineDispatcher)

        /**
         * The version of the cache.
         *
         * It is necessary to update this value when the serialization format or key transformation changes. Failure to
         * do so may result in data corruption, orphans, or other issues.
         */
        public var cacheVersion: Int = 1

        /**
         * The [KeyTransformer] used to transform the keys before they are used to store and retrieve data. It is
         * needed to avoid using invalid characters in file names.
         */
        public var keyTransformer: KeyTransformer? = SHA256KeyHasher
    }
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
