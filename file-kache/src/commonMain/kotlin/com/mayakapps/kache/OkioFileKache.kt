package com.mayakapps.kache

import com.mayakapps.kache.journal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.EOFException
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

/**
 * A persistent coroutine-safe [ContainerKache] implementation that uses [Okio](https://square.github.io/okio/) to
 * store files.
 *
 * It uses a journal file to keep track of the cache state and to ensure that the cache is always in a consistent state.
 *
 * It can be built using the following syntax:
 * ```
 * val cache = OkioFileKache(directory = "cache".toPath(), maxSize = 100L * 1024L * 1024L) {
 *     strategy = KacheStrategy.LRU
 *     // ...
 * }
 * ```
 *
 * @see Configuration
 * @see invoke
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

    override suspend fun get(key: String): Path? {
        val transformedKey = key.transform()
        val result = underlyingKache.get(transformedKey)
        if (result != null) writeRead(transformedKey)
        return result
    }

    override suspend fun getIfAvailable(key: String): Path? {
        val transformedKey = key.transform()
        val result = underlyingKache.getIfAvailable(transformedKey)
        if (result != null) writeRead(transformedKey)
        return result
    }

    override suspend fun getOrPut(key: String, creationFunction: suspend (Path) -> Boolean): Path? {
        var created = false
        val transformedKey = key.transform()
        val result = underlyingKache.getOrPut(transformedKey) {
            created = true
            wrapCreationFunction(it, creationFunction)
        }

        if (!created && result != null) writeRead(transformedKey)
        return result
    }

    override suspend fun put(key: String, creationFunction: suspend (Path) -> Boolean) =
        underlyingKache.put(key.transform()) { wrapCreationFunction(it, creationFunction) }

    override suspend fun putAsync(key: String, creationFunction: suspend (Path) -> Boolean) =
        underlyingKache.putAsync(key.transform()) { wrapCreationFunction(it, creationFunction) }

    override suspend fun remove(key: String) {
        // It's fine to consider the file is dirty now. Even if removal failed it's scheduled for
        val transformedKey = key.transform()
        writeDirty(transformedKey)
        underlyingKache.remove(transformedKey)
    }

    override suspend fun clear() {
        close()
        if (fileSystem.metadata(directory).isDirectory) fileSystem.deleteRecursively(directory)
        fileSystem.createDirectories(directory)
    }

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

    /**
     * Configuration for [OkioFileKache]. It is used as a receiver of [OkioFileKache] builder which is [invoke].
     */
    data class Configuration(
        /**
         * The directory used for storing the cached files and the journal.
         */
        var directory: Path,

        /**
         * The max size of this cache ib bytes.
         */
        var maxSize: Long,

        /**
         * The strategy used for evicting elements. See [KacheStrategy]
         */
        var strategy: KacheStrategy = KacheStrategy.LRU,

        /**
         * The file system used for storing the journal and cached files. See [FileSystem]
         */
        var fileSystem: FileSystem = getDefaultFileSystem(),

        /**
         * The coroutine dispatcher used for executing `creationFunction` of put requests.
         */
        var creationScope: CoroutineScope = CoroutineScope(getIODispatcher()),

        /**
         * The version of the entries in this cache. It is used for invalidating the cache. Update it when you change
         * the format of the entries in this cache.
         */
        var cacheVersion: Int = 1,

        /**
         * The [KeyTransformer] used to transform the keys before they are used to store and retrieve data. It is
         * needed to avoid using invalid characters in the file names.
         */
        var keyTransformer: KeyTransformer? = SHA256KeyHasher,
    )

    companion object {

        /**
         * Creates a new [OkioFileKache] with the given [directory] and [maxSize] and is configured by [configuration].
         *
         * @see Configuration
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
            } catch (ex: EOFException) {
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