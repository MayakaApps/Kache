package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class Journal private constructor(
    directory: File,
    private val fileManager: FileManager,
    private val outputStreamFactory: (File) -> OutputStream,
    initialOpsCount: Int,
    initialRedundantOpsCount: Int,
) {

    private val journalFile = getFile(directory, JOURNAL_FILE)
    private val tempJournalFile = getFile(directory, JOURNAL_FILE_TEMP)
    private val backupJournalFile = getFile(directory, JOURNAL_FILE_BACKUP)

    private val journalMutex = Mutex()
    private var journalWriter = JournalWriter(outputStreamFactory(journalFile))

    private var opsCount = initialOpsCount
    private var redundantOpsCount = initialRedundantOpsCount

    suspend fun writeClean(key: String) = journalMutex.withLock {
        journalWriter.writeClean(key)
        opsCount++
        redundantOpsCount++
    }

    suspend fun writeDirty(key: String) = journalMutex.withLock {
        journalWriter.writeDirty(key)
        opsCount++
    }

    suspend fun writeRemove(key: String) = journalMutex.withLock {
        journalWriter.writeRemove(key)
        opsCount++
        redundantOpsCount += 3
    }

    suspend fun close() = journalMutex.withLock { journalWriter.close() }

    // We only rebuild the journal when it will halve the size of the journal and eliminate at least 2000 ops.
    suspend fun rebuildJournalIfRequired(lazyKeys: suspend () -> Pair<Collection<String>, Collection<String>>) {
        if (redundantOpsCount < REDUNDANT_OPS_THRESHOLD || redundantOpsCount < opsCount / 2) return

        journalMutex.withLock {
            // Check again to make sure that there was not an ongoing rebuild request
            if (redundantOpsCount < REDUNDANT_OPS_THRESHOLD || redundantOpsCount < opsCount / 2) return

            val (cleanKeys, dirtyKeys) = lazyKeys()

            journalWriter.close()

            fileManager.deleteOrThrow(tempJournalFile)
            JournalWriter(tempJournalFile.filePath).use { writer ->
                writer.writeHeader()
                writer.writeAll(cleanKeys, dirtyKeys)
            }

            if (fileManager.exists(journalFile)) {
                fileManager.renameToOrThrow(journalFile, backupJournalFile, true)
            }
            fileManager.renameToOrThrow(tempJournalFile, journalFile, false)
            fileManager.delete(backupJournalFile)

            journalWriter = JournalWriter(journalFile.filePath)
            opsCount = cleanKeys.size + dirtyKeys.size
            redundantOpsCount = 0
        }
    }

    companion object {
        fun openOrCreate(
            directory: File,
            fileManager: FileManager,
            inputStreamFactory: (File) -> InputStream,
            outputStreamFactory: (File) -> OutputStream,
        ): Pair<Journal, List<String>> {
            val journalFile = getFile(directory, JOURNAL_FILE)
            val tempJournalFile = getFile(directory, JOURNAL_FILE_TEMP)
            val backupJournalFile = getFile(directory, JOURNAL_FILE_BACKUP)

            // If a backup file exists, use it instead.
            if (fileManager.exists(backupJournalFile)) {
                // If journal file also exists just delete backup file.
                if (fileManager.exists(journalFile)) {
                    fileManager.delete(backupJournalFile)
                } else {
                    fileManager.renameToOrThrow(backupJournalFile, journalFile, false)
                }
            }

            // If a temp file exists, delete it
            fileManager.deleteOrThrow(tempJournalFile)

            // Prefer to pick up where we left off
            val journalData = if (fileManager.exists(journalFile)) {
                val journalData = JournalReader(inputStreamFactory(journalFile)).use { it.readJournal() }
                val redundantOpsCount = journalData.opsCount - journalData.cleanKeys.size

                // Rebuild journal if required
                if (
                    journalData.isCorrupted ||
                    (redundantOpsCount >= REDUNDANT_OPS_THRESHOLD && redundantOpsCount >= journalData.cleanKeys.size)
                ) {
                    fileManager.deleteOrThrow(tempJournalFile)
                    JournalWriter(tempJournalFile.filePath).use { writer ->
                        writer.writeHeader()
                        writer.writeAll(journalData.cleanKeys, emptyList())
                    }

                    if (fileManager.exists(journalFile)) fileManager.renameToOrThrow(
                        journalFile,
                        backupJournalFile,
                        true
                    )
                    fileManager.renameToOrThrow(tempJournalFile, journalFile, false)
                    fileManager.delete(backupJournalFile)
                }

                journalData
            } else JournalReader.Result(false, emptyList(), 0)

            // Make sure that journal directory exists
            fileManager.mkdirs(directory)

            return Journal(
                directory = directory,
                fileManager = fileManager,
                outputStreamFactory = outputStreamFactory,
                initialOpsCount = journalData.opsCount,
                initialRedundantOpsCount = journalData.opsCount - journalData.cleanKeys.size,
            ) to journalData.cleanKeys
        }

        // Constants

        private const val REDUNDANT_OPS_THRESHOLD = 2000

        private const val JOURNAL_FILE = "journal"
        private const val JOURNAL_FILE_TEMP = "$JOURNAL_FILE.tmp"
        private const val JOURNAL_FILE_BACKUP = "$JOURNAL_FILE.bkp"
    }
}