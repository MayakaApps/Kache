package com.mayakapps.lrucache

import com.mayakapps.lrucache.io.*
import com.mayakapps.lrucache.journal.Journal
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock

/**
 * A persisted Least Recently Used (LRU) cache. It can be opened/created by [DiskLruCache.open]
 *
 * An LRU cache is a cache that holds strong references to a limited number of values. Each time a value is accessed,
 * it is moved to the head of a queue. When a value is added to a full cache, the value at the end of that queue is
 * evicted and may become eligible for garbage collection.
 */
class DiskLruCache private constructor(
    private val fileManager: FileManager,
    private val directory: File,
    private val journal: Journal,
    maxSize: Long,
    private val creationScope: CoroutineScope,
    private val keyTransformer: KeyTransformer?,
) {
    private val lruCache = LruCache<String, File>(
        maxSize = maxSize,
        sizeCalculator = { _, file -> fileManager.size(file) },
        onEntryRemoved = { _, key, oldValue, _ -> onEntryRemoved(key, oldValue) },
        creationScope = creationScope,
    )

    /**
     * Returns the file for [key] if it exists in the cache or wait for its creation if it is currently in progress.
     * This returns `null` if a file is not cached and isn't in creation or cannot be created.
     *
     * It may even throw exceptions for unhandled exceptions in the currently in-progress creation block.
     */
    suspend fun get(key: String): String? = lruCache.get(key.transform())?.filePath

    /**
     * Returns the file for [key] if it already exists in the cache or `null` if it doesn't exist or creation is still
     * in progress.
     */
    suspend fun getIfAvailable(key: String): String? =
        lruCache.getIfAvailable(key.transform())?.filePath

    /**
     * Returns the file for [key] if it exists in the cache, its creation is in progress or can be created by
     * [creationFunction]. If a file is returned, it'll be moved to the head of the queue. This returns `null` if a
     * file is not cached and cannot be created. You can imply that the creation has failed by returning `false`.
     * Any unhandled exceptions inside [creationFunction] won't be handled.
     */
    suspend fun getOrPut(key: String, writeFunction: suspend (String) -> Boolean) =
        lruCache.getOrPut(key.transform()) { creationFunction(it, writeFunction) }?.filePath

    /**
     * Creates a new file for [key] using [creationFunction] and returns the new value. Any existing file or
     * in-progress creation of [key] would be replaced by the new function. If a file is created, it'll be moved to the
     * head of the queue. This returns `null` if the file cannot be created. You can imply that the creation has
     * failed by returning `false`. Any unhandled exceptions inside [creationFunction] won't be handled.
     */
    suspend fun put(key: String, writeFunction: suspend (String) -> Boolean) =
        lruCache.put(key.transform()) { creationFunction(it, writeFunction) }?.filePath

    /**
     * Creates a new file for [key] using [creationFunction] and returns a [Deferred]. Any existing file or
     * in-progress creation of [key] would be replaced by the new function. If a file is created, it'll be moved to the
     * head of the queue. You can imply that the creation has failed by returning `null`.
     */
    suspend fun putAsync(key: String, writeFunction: suspend (String) -> Boolean) =
        creationScope.async {
            lruCache.putAsync(key.transform()) { creationFunction(it, writeFunction) }.await()?.filePath
        }

    /**
     * Removes the entry and in-progress creation for [key] if it exists. It returns the previous value for [key].
     */
    suspend fun remove(key: String) {
        // It's fine to consider the file is dirty now. Even if removal failed it's scheduled for
        val transformedKey = key.transform()
        journal.writeDirty(transformedKey)
        lruCache.remove(transformedKey)
    }

    /**
     * Clears the cache, calling [onEntryRemoved] on each removed entry.
     */
    suspend fun clear() {
        close()
        if (fileManager.isDirectory(directory)) fileManager.deleteRecursively(directory)
        fileManager.createDirectories(directory)
    }

    /**
     * Closes the journal file and cancels any in-progress creation.
     */
    suspend fun close() {
        lruCache.mapMutex.withLock {
            journal.close()

            for (deferred in lruCache.creationMap.values) deferred.cancel()
        }
    }


    private suspend fun String.transform() =
        keyTransformer?.transform(this) ?: this


    private suspend fun creationFunction(
        key: String,
        writeFunction: suspend (String) -> Boolean,
    ): File? {
        val tempFile = getFile(directory, key + TEMP_EXT)
        val cleanFile = getFile(directory, key)

        journal.writeDirty(key)
        return if (writeFunction(tempFile.filePath) && fileManager.exists(tempFile)) {
            fileManager.renameToOrThrow(tempFile, cleanFile, true)
            fileManager.deleteOrThrow(tempFile)
            journal.writeClean(key)
            rebuildJournalIfRequired()
            cleanFile
        } else {
            fileManager.deleteOrThrow(tempFile)
            journal.writeRemove(key)
            rebuildJournalIfRequired()
            null
        }
    }

    private fun onEntryRemoved(key: String, oldValue: File) {
        creationScope.launch {
            fileManager.deleteOrThrow(oldValue)
            fileManager.deleteOrThrow(oldValue.appendExt(TEMP_EXT))

            journal.writeRemove(key)
            rebuildJournalIfRequired()
        }
    }

    private suspend fun clearDirectory() = lruCache.mapMutex.withLock {
        val files = lruCache.map.values
        for (file in fileManager.listContent(directory) ?: emptyList()) {
            if (file !in files) fileManager.deleteOrThrow(file)
        }
    }

    private suspend fun rebuildJournalIfRequired() {
        journal.rebuildJournalIfRequired {
            lruCache.mapMutex.withLock {
                lruCache.map.keys to lruCache.creationMap.keys
            }
        }
    }

    companion object {
        /**
         * Opens the persisted Least Recently Used (LRU) cache in the provided directory or creates a new one if it
         * doesn't already exist.
         *
         * @param directoryPath The directory for storing the journal and cached files. To prevent data loss, it shouldn't be used
         * for storing files not managed by this class.
         * @param maxSize The max size of this cache in bytes, excluding the size of the journal file.
         * @param creationDispatcher The coroutine dispatcher used for executing `creationFunction` of put requests.
         * @param keyTransformer function used for transforming keys to be safe for filenames. See [KeyTransformer]
         */
        suspend fun open(
            directoryPath: String,
            maxSize: Long,
            creationDispatcher: CoroutineDispatcher,
            keyTransformer: KeyTransformer? = SHA256KeyHasher,
        ): DiskLruCache {
            require(maxSize > 0) { "maxSize must be positive value" }

            val directory = getFile(directoryPath)

            val (journal, existingKeys) = Journal.openOrCreate(
                directory = directory,
                fileManager = DefaultFileManager,
                inputStreamFactory = { BufferedInputStream(FileInputStream(it.filePath)) },
                outputStreamFactory = { BufferedOutputStream(FileOutputStream(it.filePath)) },
            )

            return open(
                DefaultFileManager,
                directory,
                journal,
                existingKeys,
                maxSize,
                creationDispatcher,
                keyTransformer,
            )
        }

        internal suspend fun open(
            fileManager: FileManager,
            directory: File,
            journal: Journal,
            existingKeys: Collection<String>,
            maxSize: Long,
            creationDispatcher: CoroutineDispatcher,
            keyTransformer: KeyTransformer? = SHA256KeyHasher,
        ): DiskLruCache {
            val creationScope = CoroutineScope(creationDispatcher)

            // Clean directory
            val directoryPath = directory.filePath
            val nameStartIndex = directoryPath.length + if (!directoryPath.endsWith("/")) 1 else 0
            for (file in fileManager.listContent(directory) ?: emptyList()) {
                if (file.filePath.substring(nameStartIndex) !in existingKeys) fileManager.deleteOrThrow(file)
            }

            val cache = DiskLruCache(fileManager, directory, journal, maxSize, creationScope, keyTransformer)

            for (key in existingKeys) cache.lruCache.put(key, getFile(directory, key))

            return cache
        }

        private const val TEMP_EXT = ".tmp"
    }
}