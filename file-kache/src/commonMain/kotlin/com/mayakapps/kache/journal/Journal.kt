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

package com.mayakapps.kache.journal

import com.mayakapps.kache.KacheStrategy
import com.mayakapps.kache.atomicMove
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

internal data class JournalData(
    val cleanEntries: LinkedHashMap<String, String?>,
    val dirtyEntryKeys: Set<String>,
    val redundantEntriesCount: Int,
)

internal fun FileSystem.readJournalIfExists(
    directory: Path,
    cacheVersion: Int = 1,
    strategy: KacheStrategy = KacheStrategy.LRU,
): JournalData? {
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
    val dirtyEntryKeys = mutableSetOf<String>()
    val cleanEntries = linkedMapOf<String, String?>()

    JournalReader(source(journalFile).buffer(), cacheVersion, strategy).use { reader ->
        reader.validateHeader()

        while (true) {
            val entry = reader.readEntry() ?: break
            entriesCount++

            when (entry) {
                is JournalEntry.Dirty -> {
                    dirtyEntryKeys.add(entry.key)
                }

                is JournalEntry.Clean -> {
                    // Remove existing entry to re-insert it at the end of the map
                    val transformedKey = cleanEntries.remove(entry.key)

                    dirtyEntryKeys.remove(entry.key)
                    cleanEntries[entry.key] = transformedKey
                }

                is JournalEntry.CleanWithTransformedKey -> {
                    // Remove existing entry to re-insert it at the end of the map
                    cleanEntries.remove(entry.key)

                    dirtyEntryKeys.remove(entry.key)
                    cleanEntries[entry.key] = entry.transformedKey
                }

                is JournalEntry.Cancel -> {
                    dirtyEntryKeys.remove(entry.key)
                }

                is JournalEntry.Remove -> {
                    dirtyEntryKeys.remove(entry.key)
                    cleanEntries.remove(entry.key)
                }

                is JournalEntry.Read -> {
                    if (strategy == KacheStrategy.LRU || strategy == KacheStrategy.MRU) {
                        // Remove existing entry to re-insert it at the end of the map
                        val transformedKey = cleanEntries.remove(entry.key)
                        cleanEntries[entry.key] = transformedKey
                    }
                }
            }
        }
    }

    return JournalData(
        cleanEntries = cleanEntries,
        dirtyEntryKeys = dirtyEntryKeys,
        redundantEntriesCount = entriesCount - cleanEntries.size,
    )
}

internal fun FileSystem.writeJournalAtomically(
    directory: Path,
    cleanEntries: Map<String, String>,
    dirtyEntryKeys: Collection<String>
) {
    val journalFile = directory.resolve(JOURNAL_FILE)
    val tempJournalFile = directory.resolve(JOURNAL_FILE_TEMP)
    val backupJournalFile = directory.resolve(JOURNAL_FILE_BACKUP)

    delete(tempJournalFile)

    JournalWriter(sink(tempJournalFile, mustCreate = true).buffer()).use { writer ->
        writer.writeHeader()
        writer.writeAll(cleanEntries, dirtyEntryKeys)
    }

    if (exists(journalFile)) atomicMove(journalFile, backupJournalFile, deleteTarget = true)
    atomicMove(tempJournalFile, journalFile)
    delete(backupJournalFile)
}
