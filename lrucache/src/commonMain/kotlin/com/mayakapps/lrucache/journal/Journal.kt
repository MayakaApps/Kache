package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class Journal private constructor(
    private val directory: File,
    private val fileManager: FileManager,
    initialRedundantOpsCount: Int,
) {

    private val journalFile = getFile(directory, JOURNAL_FILE)

    private val journalMutex = Mutex()
    private var journalWriter = JournalWriter(BufferedOutputStream(fileManager.outputStream(journalFile)))

    private var redundantOpsCount = initialRedundantOpsCount

    suspend fun writeClean(key: String) = journalMutex.withLock {
        journalWriter.writeClean(key)
        redundantOpsCount++
    }

    suspend fun writeDirty(key: String) = journalMutex.withLock {
        journalWriter.writeDirty(key)
    }

    suspend fun writeRemove(key: String) = journalMutex.withLock {
        journalWriter.writeRemove(key)
        redundantOpsCount += 3
    }

    suspend fun close() = journalMutex.withLock { journalWriter.close() }

    // We only rebuild the journal when it will halve the size of the journal and eliminate at least 2000 ops.
    suspend fun rebuildJournalIfRequired(lazyKeys: suspend () -> Pair<Collection<String>, Collection<String>>) {
        if (redundantOpsCount < REDUNDANT_ENTRIES_THRESHOLD) return

        journalMutex.withLock {
            // Check again to make sure that there was not an ongoing rebuild request
            if (redundantOpsCount < REDUNDANT_ENTRIES_THRESHOLD) return

            val (cleanKeys, dirtyKeys) = lazyKeys()

            journalWriter.close()

            fileManager.writeJournalAtomically(directory, cleanKeys, dirtyKeys)

            journalWriter = JournalWriter(BufferedOutputStream(fileManager.outputStream(journalFile)))
            redundantOpsCount = 0
        }
    }

    companion object {
        fun openOrCreate(
            directory: File,
            fileManager: FileManager,
        ): Pair<Journal, List<String>> {
            val journalData = try {
                fileManager.readJournalIfExists(directory)
            } catch (ex: JournalException) {
                // Journal is corrupted - Clear cache
                fileManager.deleteContentsOrThrow(directory)
                null
            }

            // Make sure that journal directory exists
            fileManager.createDirectories(directory)

            // Rebuild journal if required
            val redundantEntriesCount = if (journalData == null) {
                fileManager.writeJournalAtomically(directory, emptyList(), emptyList())
                0
            } else if (
                journalData.redundantEntriesCount >= REDUNDANT_ENTRIES_THRESHOLD &&
                journalData.redundantEntriesCount >= journalData.cleanEntriesKeys.size
            ) {
                fileManager
                    .writeJournalAtomically(directory, journalData.cleanEntriesKeys, journalData.dirtyEntriesKeys)
                0
            } else journalData.redundantEntriesCount

            return Journal(
                directory = directory,
                fileManager = fileManager,
                initialRedundantOpsCount = redundantEntriesCount,
            ) to journalData?.cleanEntriesKeys.orEmpty()
        }

        // Constants

        private const val REDUNDANT_ENTRIES_THRESHOLD = 2000
    }
}

internal data class JournalData(
    val cleanEntriesKeys: List<String>,
    val dirtyEntriesKeys: List<String>,
    val redundantEntriesCount: Int,
)

internal fun FileManager.readJournalIfExists(directory: File): JournalData? {
    val journalFile = getFile(directory, JOURNAL_FILE)
    val tempJournalFile = getFile(directory, JOURNAL_FILE_TEMP)
    val backupJournalFile = getFile(directory, JOURNAL_FILE_BACKUP)

    // If a backup file exists, use it instead.
    if (exists(backupJournalFile)) {
        // If journal file also exists just delete backup file.
        if (exists(journalFile)) {
            delete(backupJournalFile)
        } else {
            renameToOrThrow(backupJournalFile, journalFile, false)
        }
    }

    // If a temp file exists, delete it
    deleteOrThrow(tempJournalFile)

    if (!exists(journalFile)) return null

    var entriesCount = 0
    val dirtyEntriesKeys = mutableListOf<String>()
    val cleanEntriesKeys = mutableListOf<String>()

    JournalReader(BufferedInputStream(inputStream(journalFile))).use { reader ->
        reader.validateHeader()

        while (true) {
            val entry = reader.readEntry() ?: break
            entriesCount++

            when (entry) {
                is JournalEntry.Dirty -> {
                    dirtyEntriesKeys += entry.key
                }

                is JournalEntry.Clean -> {
                    dirtyEntriesKeys.remove(entry.key)
                    cleanEntriesKeys += entry.key
                }

                is JournalEntry.Remove -> {
                    dirtyEntriesKeys.remove(entry.key)
                    cleanEntriesKeys.remove(entry.key)
                }
            }
        }
    }

    return JournalData(
        cleanEntriesKeys = cleanEntriesKeys,
        dirtyEntriesKeys = dirtyEntriesKeys,
        redundantEntriesCount = entriesCount - cleanEntriesKeys.size,
    )
}

internal fun FileManager.writeJournalAtomically(
    directory: File,
    cleanEntriesKeys: Collection<String>,
    dirtyEntriesKeys: Collection<String>
) {
    val journalFile = getFile(directory, JOURNAL_FILE)
    val tempJournalFile = getFile(directory, JOURNAL_FILE_TEMP)
    val backupJournalFile = getFile(directory, JOURNAL_FILE_BACKUP)

    deleteOrThrow(tempJournalFile)

    JournalWriter(BufferedOutputStream(outputStream(tempJournalFile))).use { writer ->
        writer.writeHeader()
        writer.writeAll(cleanEntriesKeys, dirtyEntriesKeys)
    }

    if (exists(journalFile)) renameToOrThrow(journalFile, backupJournalFile, deleteDest = true)
    renameToOrThrow(tempJournalFile, journalFile, deleteDest = false)
    delete(backupJournalFile)
}