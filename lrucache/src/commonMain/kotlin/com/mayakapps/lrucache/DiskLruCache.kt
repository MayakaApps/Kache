package com.mayakapps.lrucache

import com.mayakapps.lrucache.io.*
import com.mayakapps.lrucache.journal.JournalOp
import com.mayakapps.lrucache.journal.JournalReader
import com.mayakapps.lrucache.journal.JournalWriter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A persisted Least Recently Used (LRU) cache. It can be opened/created by [DiskLruCache.open]
 *
 * An LRU cache is a cache that holds strong references to a limited number of values. Each time a value is accessed,
 * it is moved to the head of a queue. When a value is added to a full cache, the value at the end of that queue is
 * evicted and may become eligible for garbage collection.
 */
class DiskLruCache private constructor(
    private val directoryPath: String,
    maxSize: Long,
    private val creationScope: CoroutineScope,
    private val keyTransformer: KeyTransformer?,
) {
    private val journalFile = Files.appendPath(directoryPath, JOURNAL_FILE)
    private val tempJournalFile = Files.appendPath(directoryPath, JOURNAL_FILE_TEMP)
    private val backupJournalFile = Files.appendPath(directoryPath, JOURNAL_FILE_BACKUP)

    private val lruCache = LruCache<String, String>(
        maxSize = maxSize,
        sizeCalculator = { _, file -> Files.size(file) },
        onEntryRemoved = { _, key, oldValue, _ -> onEntryRemoved(key, oldValue) },
        creationScope = creationScope,
    )

    private var redundantOpCount = 0
    private lateinit var journalWriter: JournalWriter
    private val journalMutex = Mutex()

    /**
     * Returns the file for [key] if it exists in the cache or wait for its creation if it is currently in progress.
     * This returns `null` if a file is not cached and isn't in creation or cannot be created.
     *
     * It may even throw exceptions for unhandled exceptions in the currently in-progress creation block.
     */
    suspend fun get(key: String): String? = lruCache.get(key.transform())

    /**
     * Returns the file for [key] if it already exists in the cache or `null` if it doesn't exist or creation is still
     * in progress.
     */
    suspend fun getIfAvailable(key: String): String? =
        lruCache.getIfAvailable(key.transform())

    /**
     * Returns the file for [key] if it exists in the cache, its creation is in progress or can be created by
     * [creationFunction]. If a file is returned, it'll be moved to the head of the queue. This returns `null` if a
     * file is not cached and cannot be created. You can imply that the creation has failed by returning `false`.
     * Any unhandled exceptions inside [creationFunction] won't be handled.
     */
    suspend fun getOrPut(key: String, writeFunction: suspend (String) -> Boolean) =
        lruCache.getOrPut(key.transform()) { creationFunction(it, writeFunction) }

    /**
     * Creates a new file for [key] using [creationFunction] and returns the new value. Any existing file or
     * in-progress creation of [key] would be replaced by the new function. If a file is created, it'll be moved to the
     * head of the queue. This returns `null` if the file cannot be created. You can imply that the creation has
     * failed by returning `false`. Any unhandled exceptions inside [creationFunction] won't be handled.
     */
    suspend fun put(key: String, writeFunction: suspend (String) -> Boolean) =
        lruCache.put(key.transform()) { creationFunction(it, writeFunction) }

    /**
     * Creates a new file for [key] using [creationFunction] and returns a [Deferred]. Any existing file or
     * in-progress creation of [key] would be replaced by the new function. If a file is created, it'll be moved to the
     * head of the queue. You can imply that the creation has failed by returning `null`.
     */
    suspend fun putAsync(key: String, writeFunction: suspend (String) -> Boolean) =
        lruCache.putAsync(key.transform()) { creationFunction(it, writeFunction) }

    /**
     * Removes the entry and in-progress creation for [key] if it exists. It returns the previous value for [key].
     */
    suspend fun remove(key: String) {
        // It's fine to consider the file is dirty now. Even if removal failed it's scheduled for
        journalMutex.withLock {
            journalWriter.writeDirty(key.transform())
            redundantOpCount++
        }

        lruCache.remove(key.transform())
    }

    /**
     * Clears the cache, calling [onEntryRemoved] on each removed entry.
     */
    suspend fun clear() {
        close()
        if (Files.isDirectory(directoryPath)) Files.deleteRecursively(directoryPath)
        Files.mkdirs(directoryPath)
    }

    /**
     * Closes the journal file and cancels any in-progress creation.
     */
    suspend fun close() {
        lruCache.mapMutex.withLock {
            if (::journalWriter.isInitialized) {
                journalMutex.withLock {
                    journalWriter.close()
                }
            }

            for (deferred in lruCache.creationMap.values) deferred.cancel()
        }
    }


    private suspend fun String.transform() =
        keyTransformer?.transform(this) ?: this


    private suspend fun creationFunction(
        key: String,
        writeFunction: suspend (String) -> Boolean,
    ): String? {
        val tempFile = Files.appendPath(directoryPath, key + TEMP_EXT)
        val cleanFile = Files.appendPath(directoryPath, key)

        journalMutex.withLock { journalWriter.writeDirty(key) }
        if (writeFunction(tempFile) && Files.exists(tempFile)) {
            Files.renameToOrThrow(tempFile, cleanFile, true)
            Files.deleteOrThrow(tempFile)
            journalMutex.withLock {
                journalWriter.writeClean(key)
                redundantOpCount++
            }
            rebuildJournalIfRequired()
            return cleanFile
        } else {
            Files.deleteOrThrow(tempFile)
            journalMutex.withLock {
                journalWriter.writeRemove(key)
                redundantOpCount += 2
            }
            rebuildJournalIfRequired()
            return null
        }
    }

    private fun onEntryRemoved(key: String, oldValue: String) {
        creationScope.launch {
            Files.deleteOrThrow(oldValue)
            Files.deleteOrThrow(oldValue + TEMP_EXT)

            if (::journalWriter.isInitialized) {
                journalMutex.withLock {
                    redundantOpCount += 2
                    journalWriter.writeRemove(key)
                }

                rebuildJournalIfRequired()
            }
        }
    }


    private suspend fun parseJournal() {
        val reader = JournalReader(journalFile)
        val allOps = reader.use { it.readFully() }
        val opsByKey = allOps.groupBy { it.key }

        val readEntries = mutableListOf<Pair<String, String>>()
        for ((key, ops) in opsByKey) {
            val operation = ops.last()
            val file = Files.appendPath(directoryPath, key)
            val tempFile = Files.appendPath(directoryPath, key + TEMP_EXT)

            when (operation) {
                is JournalOp.Clean -> {
                    if (Files.exists(file)) {
                        readEntries.add(key to file)
                    }
                }

                is JournalOp.Dirty -> {
                    Files.deleteOrThrow(file)
                    Files.deleteOrThrow(tempFile)
                }

                is JournalOp.Remove -> {
                    // Do nothing
                }
            }
        }

        redundantOpCount = allOps.size - lruCache.map.size

        if (reader.isCorrupted) {
            readEntries.forEach { (key, file) -> lruCache.put(key, file) }
            clearDirectory()
            rebuildJournal()
        } else {
            // Initialize writer first to record REMOVE operations when trimming
            journalWriter = JournalWriter(journalFile)
            readEntries.forEach { (key, file) -> lruCache.put(key, file) }
        }
    }

    private suspend fun clearDirectory() = lruCache.mapMutex.withLock {
        val files = lruCache.map.values
        for (file in Files.listContent(directoryPath) ?: emptyList()) {
            if (file !in files) Files.deleteOrThrow(file)
        }
    }

    private suspend fun rebuildJournalIfRequired() {
        if (isJournalRebuildRequired) rebuildJournal()
    }

    // We only rebuild the journal when it will halve the size of the journal and eliminate at least 2000 ops.
    private val isJournalRebuildRequired: Boolean
        get() {
            val redundantOpCompactThreshold = 2000
            return (redundantOpCount >= redundantOpCompactThreshold
                    && redundantOpCount >= lruCache.map.size)
        }

    private suspend fun rebuildJournal() = lruCache.mapMutex.withLock {
        journalMutex.withLock {
            if (::journalWriter.isInitialized) journalWriter.close()

            Files.deleteOrThrow(tempJournalFile)
            JournalWriter(tempJournalFile).use { tempWriter ->
                tempWriter.writeHeader()

                lruCache.map.forEach { (key, _) ->
                    tempWriter.writeClean(key)
                }

                lruCache.creationMap.keys.forEach { key -> tempWriter.writeDirty(key) }
            }

            if (Files.exists(journalFile)) Files.renameToOrThrow(journalFile, backupJournalFile, true)
            Files.renameToOrThrow(tempJournalFile, journalFile, false)
            Files.delete(backupJournalFile)

            journalWriter = JournalWriter(journalFile)
            redundantOpCount = 0
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

            val creationScope = CoroutineScope(creationDispatcher)

            val journalFile = Files.appendPath(directoryPath, JOURNAL_FILE)
            val tempJournalFile = Files.appendPath(directoryPath, JOURNAL_FILE_TEMP)
            val backupJournalFile = Files.appendPath(directoryPath, JOURNAL_FILE_BACKUP)

            // If a backup file exists, use it instead.
            if (Files.exists(backupJournalFile)) {
                // If journal file also exists just delete backup file.
                if (Files.exists(journalFile)) {
                    Files.delete(backupJournalFile)
                } else {
                    Files.renameToOrThrow(backupJournalFile, journalFile, false)
                }
            }

            // If a temp file exists, delete it
            Files.deleteOrThrow(tempJournalFile)

            // Prefer to pick up where we left off.
            if (Files.exists(Files.appendPath(directoryPath, JOURNAL_FILE))) {
                val cache = DiskLruCache(directoryPath, maxSize, creationScope, keyTransformer)
                try {
                    cache.parseJournal()
                    return cache
                } catch (journalIsCorrupt: IOException) {
                    println(
                        "DiskLruCache "
                                + directoryPath
                                + " is corrupt: "
                                + journalIsCorrupt.message
                                + ", removing"
                    )
                    cache.clear()
                }
            }

            // Create a new empty cache.
            Files.mkdirs(directoryPath)
            val cache = DiskLruCache(directoryPath, maxSize, creationScope, keyTransformer)
            cache.rebuildJournal()
            return cache
        }

        private const val TEMP_EXT = ".tmp"
        private const val JOURNAL_FILE = "journal"
        private const val JOURNAL_FILE_TEMP = JOURNAL_FILE + TEMP_EXT
        private const val JOURNAL_FILE_BACKUP = "$JOURNAL_FILE.bkp"
    }
}