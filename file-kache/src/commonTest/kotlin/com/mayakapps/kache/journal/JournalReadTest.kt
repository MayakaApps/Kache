/*
 * Copyright 2023 MayakaApps
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

import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.use
import kotlin.test.*

class JournalReadTest {

    @Test
    fun readNonExistingJournal() {
        val fileSystem = FakeFileSystem()

        assertNull(fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun readSimpleJournal() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAdd) }

        assertEquals(journalWithAddData, fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun readSimpleBackupJournal() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(backupJournalFile).buffer().use { it.write(journalWithAdd) }

        assertEquals(journalWithAddData, fileSystem.readJournalIfExists(directory))
        assertTrue(fileSystem.exists(journalFile))
        assertFalse(fileSystem.exists(backupJournalFile))
    }

    @Test
    fun readJournalAlongWithTempAndBackupJournal() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAdd) }
        fileSystem.sink(tempJournalFile).buffer().use { it.write(emptyJournal) }
        fileSystem.sink(backupJournalFile).buffer().use { it.write(emptyJournal) }

        assertEquals(journalWithAddData, fileSystem.readJournalIfExists(directory))
        assertTrue(fileSystem.exists(journalFile))
        assertFalse(fileSystem.exists(tempJournalFile))
        assertFalse(fileSystem.exists(backupJournalFile))
    }

    @Test
    fun readJournalWithDirtyAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithDirty) }

        assertEquals(journalWithDirtyData, fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun readJournalWithAddingAndRemoving() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAddAndRemove) }

        assertEquals(journalWithAddAndRemoveData, fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun readJournalWithMixedAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithMixedAdd) }

        assertEquals(journalWithMixedAddData, fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun readJournalWithAddingAndReAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAddAndReAdd) }

        assertEquals(journalWithAddAndReAddData, fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun readJournalWithAddingAndDirtyAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAddAndDirtyAdd) }

        assertEquals(journalWithAddAndDirtyAddData, fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun readJournalWithAddingAndCancelledAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAddAndCancelledAdd) }

        assertEquals(journalWithAddAndCancelledAddData, fileSystem.readJournalIfExists(directory))
    }

    companion object {
        private const val KEY_1 = "one"
        private const val KEY_2 = "two"

        private val directory = "/cache/".toPath()
        private val journalFile = directory.resolve(JOURNAL_FILE)
        private val tempJournalFile = directory.resolve(JOURNAL_FILE_TEMP)
        private val backupJournalFile = directory.resolve(JOURNAL_FILE_BACKUP)

        private val keyOneBytes = byteArrayOf(KEY_1.length.toByte()) + KEY_1.encodeToByteArray()
        private val keyTwoBytes = byteArrayOf(KEY_2.length.toByte()) + KEY_2.encodeToByteArray()

        private val journalHeader =
            byteArrayOf(0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, JOURNAL_VERSION, 0x00, 0x00, 0x00, 0x01)
        private val emptyJournal = journalHeader

        private val journalWithDirty =
            byteArrayOf(*journalHeader, JournalEntry.DIRTY, *keyOneBytes, JournalEntry.DIRTY, *keyTwoBytes)

        private val journalWithDirtyData = JournalData(
            cleanEntriesKeys = emptyList(),
            dirtyEntriesKeys = listOf(KEY_1, KEY_2),
            redundantEntriesCount = 2,
        )

        private val journalWithAdd =
            byteArrayOf(*journalHeader, JournalEntry.DIRTY, *keyOneBytes, JournalEntry.CLEAN, *keyOneBytes)

        private val journalWithAddData = JournalData(
            cleanEntriesKeys = listOf(KEY_1),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 1,
        )

        private val journalWithMixedAdd =
            byteArrayOf(
                *journalWithDirty,
                JournalEntry.CLEAN, *keyOneBytes,
                JournalEntry.CLEAN, *keyTwoBytes,
            )
        private val journalWithMixedAddData = JournalData(
            cleanEntriesKeys = listOf(KEY_1, KEY_2),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 2,
        )

        private val journalWithAddAndRemove =
            byteArrayOf(
                *journalWithAdd,
                JournalEntry.DIRTY, *keyOneBytes,
                JournalEntry.REMOVE, *keyOneBytes,
            )
        private val journalWithAddAndRemoveData = JournalData(
            cleanEntriesKeys = emptyList(),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 4,

            )

        private val journalWithAddAndReAdd =
            byteArrayOf(
                *journalWithAdd,
                JournalEntry.DIRTY, *keyOneBytes,
                JournalEntry.CLEAN, *keyOneBytes,
            )
        private val journalWithAddAndReAddData = JournalData(
            cleanEntriesKeys = listOf(KEY_1),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 3,
        )

        private val journalWithAddAndDirtyAdd =
            byteArrayOf(
                *journalWithAdd,
                JournalEntry.DIRTY, *keyOneBytes,
            )
        private val journalWithAddAndDirtyAddData = JournalData(
            cleanEntriesKeys = listOf(KEY_1),
            dirtyEntriesKeys = listOf(KEY_1),
            redundantEntriesCount = 2,
        )

        private val journalWithAddAndCancelledAdd =
            byteArrayOf(
                *journalWithAdd,
                JournalEntry.DIRTY, *keyOneBytes,
                JournalEntry.CANCEL, *keyOneBytes,
            )

        private val journalWithAddAndCancelledAddData = JournalData(
            cleanEntriesKeys = listOf(KEY_1),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 3,
        )
    }
}
