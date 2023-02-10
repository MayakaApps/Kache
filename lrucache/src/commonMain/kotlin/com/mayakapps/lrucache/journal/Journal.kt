package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.*

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