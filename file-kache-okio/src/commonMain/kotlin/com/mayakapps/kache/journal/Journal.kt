package com.mayakapps.kache.journal

import com.mayakapps.kache.atomicMove
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

internal data class JournalData(
    val cleanEntriesKeys: List<String>,
    val dirtyEntriesKeys: List<String>,
    val redundantEntriesCount: Int,
)

internal fun FileSystem.readJournalIfExists(directory: Path): JournalData? {
    val journalFile = directory.resolve(JOURNAL_FILE)
    val tempJournalFile = directory.resolve(JOURNAL_FILE_TEMP)
    val backupJournalFile = directory.resolve(JOURNAL_FILE_BACKUP)

    // If a backup file exists, use it instead.
    if (exists(backupJournalFile)) {
        // If journal file also exists just delete backup file.
        if (exists(journalFile)) {
            delete(backupJournalFile)
        } else {
            atomicMove(backupJournalFile, journalFile)
        }
    }

    // If a temp file exists, delete it
    delete(tempJournalFile)

    if (!exists(journalFile)) return null

    var entriesCount = 0
    val dirtyEntriesKeys = mutableListOf<String>()
    val cleanEntriesKeys = mutableListOf<String>()

    JournalReader(source(journalFile).buffer()).use { reader ->
        reader.validateHeader()

        while (true) {
            val entry = reader.readEntry() ?: break
            entriesCount++

            when (entry) {
                is JournalEntry.Dirty -> {
                    dirtyEntriesKeys += entry.key
                }

                is JournalEntry.Clean -> {
                    // Remove existing entry if it exists to avoid duplicates
                    cleanEntriesKeys.remove(entry.key)

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

internal fun FileSystem.writeJournalAtomically(
    directory: Path,
    cleanEntriesKeys: Collection<String>,
    dirtyEntriesKeys: Collection<String>
) {
    val journalFile = directory.resolve(JOURNAL_FILE)
    val tempJournalFile = directory.resolve(JOURNAL_FILE_TEMP)
    val backupJournalFile = directory.resolve(JOURNAL_FILE_BACKUP)

    delete(tempJournalFile)

    JournalWriter(sink(tempJournalFile, mustCreate = true).buffer()).use { writer ->
        writer.writeHeader()
        writer.writeAll(cleanEntriesKeys, dirtyEntriesKeys)
    }

    if (exists(journalFile)) atomicMove(journalFile, backupJournalFile, deleteTarget = true)
    atomicMove(tempJournalFile, journalFile)
    delete(backupJournalFile)
}