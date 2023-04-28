package com.mayakapps.kache

import com.mayakapps.kache.journal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

/**
 * A persisted Least Recently Used (LRU) cache. It can be opened/created by [OkioFileKache.open]
 *
 * An LRU cache is a cache that holds strong references to a limited number of values. Each time a value is accessed,
 * it is moved to the head of a queue. When a value is added to a full cache, the value at the end of that queue is
 * evicted and may become eligible for garbage collection.
 */
class OkioFileKache private constructor(
    private val fileSystem: FileSystem,
    private val directory: Path,
    maxSize: Long,
    strategy: KacheStrategy,
    private val creationScope: CoroutineScope,
    private val keyTransformer: KeyTransformer?,
    initialRedundantJournalEntriesCount: Int,
) : ContainerKache<String, Path> {

    // Explicit type parameter is a workaround for https://youtrack.jetbrains.com/issue/KT-53109
    @Suppress("RemoveExplicitTypeArguments")
    private val underlyingKache = InMemoryKache<String, Path>(maxSize = maxSize) {
        this.strategy = strategy
        this.sizeCalculator = { _, file -> fileSystem.metadata(file).size ?: 0 }
        this.onEntryRemoved = { _, key, oldValue, _ -> onEntryRemoved(key, oldValue) }
        this.creationScope = this@OkioFileKache.creationScope
    }

    private val journalMutex = Mutex()
    private val journalFile = directory.resolve(JOURNAL_FILE)
    private var journalWriter =
        JournalWriter(fileSystem.appendingSink(journalFile, mustExist = true).buffer())

    private var redundantJournalEntriesCount = initialRedundantJournalEntriesCount

    /**
     * Returns the file for [key] if it exists in the cache or wait for its creation if it is currently in progress.
     * This returns `null` if a file is not cached and isn't in creation or cannot be created.
     *
     * It may even throw exceptions for unhandled exceptions in the currently in-progress creation block.
     */
    override suspend fun get(key: String): Path? {
        val result = underlyingKache.get(key.transform())
        if (result != null) writeRead(key)
        return result
    }

    /**
     * Returns the file for [key] if it already exists in the cache or `null` if it doesn't exist or creation is still
     * in progress.
     */
    override suspend fun getIfAvailable(key: String): Path? {
        val result = underlyingKache.getIfAvailable(key.transform())
        if (result != null) writeRead(key)
        return result
    }

    /**
     * Returns the file for [key] if it exists in the cache, its creation is in progress or can be created by
     * [wrapCreationFunction]. If a file is returned, it'll be moved to the head of the queue. This returns `null` if a
     * file is not cached and cannot be created. You can imply that the creation has failed by returning `false`.
     * Any unhandled exceptions inside [wrapCreationFunction] won't be handled.
     */
    override suspend fun getOrPut(key: String, creationFunction: suspend (Path) -> Boolean): Path? {
        var created = false
        val result = underlyingKache.getOrPut(key.transform()) {
            created = true
            wrapCreationFunction(it, creationFunction)
        }

        if (!created && result != null) writeRead(key)
        return result
    }

    /**
     * Creates a new file for [key] using [wrapCreationFunction] and returns the new value. Any existing file or
     * in-progress creation of [key] would be replaced by the new function. If a file is created, it'll be moved to the
     * head of the queue. This returns `null` if the file cannot be created. You can imply that the creation has
     * failed by returning `false`. Any unhandled exceptions inside [wrapCreationFunction] won't be handled.
     */
    override suspend fun put(key: String, creationFunction: suspend (Path) -> Boolean) =
        underlyingKache.put(key.transform()) { wrapCreationFunction(it, creationFunction) }

    /**
     * Creates a new file for [key] using [wrapCreationFunction] and returns a [Deferred]. Any existing file or
     * in-progress creation of [key] would be replaced by the new function. If a file is created, it'll be moved to the
     * head of the queue. You can imply that the creation has failed by returning `null`.
     */
    override suspend fun putAsync(key: String, creationFunction: suspend (Path) -> Boolean) =
        underlyingKache.putAsync(key.transform()) { wrapCreationFunction(it, creationFunction) }

    /**
     * Removes the entry and in-progress creation for [key] if it exists. It returns the previous value for [key].
     */
    override suspend fun remove(key: String) {
        // It's fine to consider the file is dirty now. Even if removal failed it's scheduled for
        val transformedKey = key.transform()
        writeDirty(transformedKey)
        underlyingKache.remove(transformedKey)
    }

    /**
     * Clears the cache, calling [onEntryRemoved] on each removed entry.
     */
    override suspend fun clear() {
        close()
        if (fileSystem.metadata(directory).isDirectory) fileSystem.deleteRecursively(directory)
        fileSystem.createDirectories(directory)
    }

    /**
     * Closes the journal file and cancels any in-progress creation.
     */
    override suspend fun close() {
        underlyingKache.removeAllUnderCreation()
        journalMutex.withLock { journalWriter.close() }
    }

    private suspend fun String.transform() =
        keyTransformer?.transform(this) ?: this

    private suspend fun wrapCreationFunction(
        key: String,
        creationFunction: suspend (Path) -> Boolean,
    ): Path? {
        val tempFile = directory.resolve(key + TEMP_EXT)
        val cleanFile = directory.resolve(key)

        writeDirty(key)
        return if (creationFunction(tempFile) && fileSystem.exists(tempFile)) {
            fileSystem.atomicMove(tempFile, cleanFile, deleteTarget = true)
            fileSystem.delete(tempFile)
            writeClean(key)
            rebuildJournalIfRequired()
            cleanFile
        } else {
            fileSystem.delete(tempFile)
            writeCancel(key)
            rebuildJournalIfRequired()
            null
        }
    }

    private fun onEntryRemoved(key: String, oldValue: Path) {
        creationScope.launch {
            fileSystem.delete(oldValue)
            fileSystem.delete((oldValue.toString() + TEMP_EXT).toPath())

            writeRemove(key)
            rebuildJournalIfRequired()
        }
    }

    private suspend fun writeDirty(key: String) = journalMutex.withLock {
        journalWriter.writeDirty(key)
    }

    private suspend fun writeClean(key: String) = journalMutex.withLock {
        journalWriter.writeClean(key)
        redundantJournalEntriesCount++
    }

    private suspend fun writeCancel(key: String) = journalMutex.withLock {
        journalWriter.writeCancel(key)
        redundantJournalEntriesCount += 2
    }

    private suspend fun writeRemove(key: String) = journalMutex.withLock {
        journalWriter.writeRemove(key)
        redundantJournalEntriesCount += 3
    }

    private suspend fun writeRead(key: String) = journalMutex.withLock {
        journalWriter.writeRead(key)
        redundantJournalEntriesCount += 1
    }

    private suspend fun rebuildJournalIfRequired() {
        if (redundantJournalEntriesCount < REDUNDANT_ENTRIES_THRESHOLD) return

        journalMutex.withLock {
            // Check again to make sure that there was not an ongoing rebuild request
            if (redundantJournalEntriesCount < REDUNDANT_ENTRIES_THRESHOLD) return

            journalWriter.close()

            val (cleanKeys, dirtyKeys) = underlyingKache.getAllKeys()
            fileSystem.writeJournalAtomically(directory, cleanKeys, dirtyKeys)

            journalWriter =
                JournalWriter(fileSystem.appendingSink(journalFile, mustExist = true).buffer())
            redundantJournalEntriesCount = 0
        }
    }

    data class Configuration(
        var directory: Path,
        var maxSize: Long,
        var strategy: KacheStrategy = KacheStrategy.LRU,
        var fileSystem: FileSystem = defaultFileSystem,
        var creationScope: CoroutineScope = CoroutineScope(ioDispatcher),
        var cacheVersion: Int = 1,
        var keyTransformer: KeyTransformer? = SHA256KeyHasher,
    )

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
        suspend operator fun invoke(
            directory: Path,
            maxSize: Long,
            configuration: Configuration.() -> Unit = {},
        ): OkioFileKache {
            val config = Configuration(
                directory = directory,
                maxSize = maxSize,
            ).apply(configuration)

            return open(
                fileSystem = config.fileSystem,
                directory = config.directory,
                maxSize = config.maxSize,
                strategy = config.strategy,
                creationScope = config.creationScope,
                cacheVersion = config.cacheVersion,
                keyTransformer = config.keyTransformer,
            )
        }

        internal suspend fun open(
            fileSystem: FileSystem,
            directory: Path,
            maxSize: Long,
            strategy: KacheStrategy,
            creationScope: CoroutineScope,
            cacheVersion: Int = 1,
            keyTransformer: KeyTransformer? = SHA256KeyHasher,
        ): OkioFileKache {
            require(maxSize > 0) { "maxSize must be positive value" }

            // Make sure that journal directory exists
            fileSystem.createDirectories(directory)

            val journalData = try {
                fileSystem.readJournalIfExists(directory, cacheVersion)
            } catch (ex: JournalException) {
                // Journal is corrupted - Clear cache
                fileSystem.deleteContents(directory)
                null
            }

            // Delete dirty entries
            if (journalData != null) {
                for (key in journalData.dirtyEntriesKeys) {
                    fileSystem.delete(directory.resolve(key + TEMP_EXT))
                }
            }

            // Rebuild journal if required
            var redundantJournalEntriesCount = journalData?.redundantEntriesCount ?: 0

            if (journalData == null) {
                fileSystem.writeJournalAtomically(directory, emptyList(), emptyList())
            } else if (
                journalData.redundantEntriesCount >= REDUNDANT_ENTRIES_THRESHOLD &&
                journalData.redundantEntriesCount >= journalData.cleanEntriesKeys.size
            ) {
                fileSystem
                    .writeJournalAtomically(directory, journalData.cleanEntriesKeys, emptyList())
                redundantJournalEntriesCount = 0
            }

            val cache = OkioFileKache(
                fileSystem,
                directory,
                maxSize,
                strategy,
                creationScope,
                keyTransformer,
                redundantJournalEntriesCount,
            )

            if (journalData != null) {
                cache.underlyingKache.putAll(journalData.cleanEntriesKeys.associateWith { directory.resolve(it) })
            }

            return cache
        }

        private const val TEMP_EXT = ".tmp"
        private const val REDUNDANT_ENTRIES_THRESHOLD = 2000
    }
}