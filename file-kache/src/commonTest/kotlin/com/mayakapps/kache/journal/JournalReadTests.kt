package com.mayakapps.kache.journal

import com.mayakapps.kache.nullableUse
import okio.*
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.*

class JournalReadTests {

    @Test
    fun testValidateHeaderEmptyStream() {
        assertFailsWith<JournalInvalidHeaderException> {
            JournalReader(Buffer()).use { it.validateHeader() }
        }
    }

    @Test
    fun testValidateHeaderIncorrectFile() {
        // Text file containing "Text File"
        val bytes = byteArrayOf(0x54, 0x65, 0x78, 0x74, 0x20, 0x46, 0x69, 0x6C, 0x65)
        assertFailsWith<JournalInvalidHeaderException> {
            JournalReader(Buffer().apply { write(bytes) }).use { it.validateHeader() }
        }
    }

    @Test
    fun testValidateHeaderIncorrectVersion() {
        val bytes = byteArrayOf(
            0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, 0x00, 0x00, 0x00, 0x00, 0x01,
            0x01, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        assertFailsWith<JournalInvalidHeaderException> {
            JournalReader(Buffer().apply { write(bytes) }).use { it.validateHeader() }
        }
    }

    @Test
    fun testValidateHeaderDifferentCacheVersion() {
        val bytes = byteArrayOf(
            0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, 0x01, 0x00, 0x00, 0x00, 0x02,
            0x01, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        assertFailsWith<JournalInvalidHeaderException> {
            JournalReader(Buffer().apply { write(bytes) }).use { it.validateHeader() }
        }
    }

    @Test
    fun testValidateHeaderValidHeader() {
        val bytes = byteArrayOf(
            0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, JOURNAL_VERSION, 0x00, 0x00, 0x00, 0x01,
        )

        try {
            JournalReader(Buffer().apply { write(bytes) }).use { it.validateHeader() }
        } catch (e: JournalInvalidHeaderException) {
            fail("Header should be detected as valid")
        }
    }

    @Test
    fun testReadEntryEmptyJournal() {
        val entry = JournalReader(Buffer().apply { write(journalHeader) }).nullableUse {
            it.validateHeader()
            it.readEntry()
        }

        assertNull(entry)
    }

    @Test
    fun testReadSingleDirtyOperation() {
        val bytes = byteArrayOf(
            JournalEntry.DIRTY, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val readResult = JournalReader(Buffer().apply { write(bytes) }).use { it.readEntry() }
        assertEquals(JournalEntry.Dirty("TestKey"), readResult)
    }

    @Test
    fun testReadSingleCleanOperation() {
        val bytes = byteArrayOf(
            JournalEntry.CLEAN, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val readResult = JournalReader(Buffer().apply { write(bytes) }).use { it.readEntry() }
        assertEquals(JournalEntry.Clean("TestKey"), readResult)
    }

    @Test
    fun testReadSingleRemoveOperation() {
        val bytes = byteArrayOf(
            JournalEntry.REMOVE, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val readResult = JournalReader(Buffer().apply { write(bytes) }).use { it.readEntry() }
        assertEquals(JournalEntry.Remove("TestKey"), readResult)
    }

    @Test
    fun testReadInvalidOperation() {
        val bytes = byteArrayOf(
            (0xFE).toByte(), 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        assertFailsWith<JournalInvalidOpcodeException> {
            JournalReader(Buffer().apply { write(bytes) }).use { it.readEntry() }
        }
    }

    @Test
    fun testReadOperationWithoutKey() {
        val bytes = byteArrayOf(0x04)

        assertFailsWith<EOFException> {
            JournalReader(Buffer().apply { write(bytes) }).use { it.readEntry() }
        }
    }

    @Test
    fun testReadOperationWithTruncatedKey() {
        val bytes = byteArrayOf(
            0x04, 0x07, 0x54, 0x65, 0x73, 0x74,
        )

        assertFailsWith<EOFException> {
            JournalReader(Buffer().apply { write(bytes) }).use { it.readEntry() }
        }
    }

    @Test
    fun testClose() {
        val source = object : Source {
            var wasClosed = false

            override fun close() {
                wasClosed = true
            }

            override fun read(sink: Buffer, byteCount: Long): Long = byteCount
            override fun timeout(): Timeout = Timeout.NONE
        }

        JournalReader(source.buffer()).close()
        assertTrue(source.wasClosed)
    }

    @Test
    fun testReadNonExistingJournal() {
        val fileSystem = FakeFileSystem()

        assertNull(fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun testReadSimpleJournal() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAdd) }

        assertEquals(journalWithAddData, fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun testReadSimpleBackupJournal() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(backupJournalFile).buffer().use { it.write(journalWithAdd) }

        assertEquals(journalWithAddData, fileSystem.readJournalIfExists(directory))
        assertTrue(fileSystem.exists(journalFile))
        assertFalse(fileSystem.exists(backupJournalFile))
    }

    @Test
    fun testReadJournalAlongWithTempAndBackupJournal() {
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
    fun testReadJournalWithDirtyAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithDirty) }

        assertEquals(journalWithDirtyData, fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun testReadJournalWithAddingAndRemoving() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAddAndRemove) }

        assertEquals(journalWithAddAndRemoveData, fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun testReadJournalWithMixedAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithMixedAdd) }

        assertEquals(journalWithMixedAddData, fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun testReadJournalWithAddingAndReAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAddAndReAdd) }

        assertEquals(journalWithAddAndReAddData, fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun testReadJournalWithAddingAndDirtyAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAddAndDirtyAdd) }

        assertEquals(journalWithAddAndDirtyAddData, fileSystem.readJournalIfExists(directory))
    }

    @Test
    fun testReadJournalWithAddingAndCancelledAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAddAndCancelledAdd) }

        assertEquals(journalWithAddAndCancelledAddData, fileSystem.readJournalIfExists(directory))
    }

    // Test Data

    companion object {
        private const val KEY = "TestKey"
        private const val ALT_KEY = "AltKey"

        private val directory = "/cache/".toPath()
        private val journalFile = directory.resolve(JOURNAL_FILE)
        private val tempJournalFile = directory.resolve(JOURNAL_FILE_TEMP)
        private val backupJournalFile = directory.resolve(JOURNAL_FILE_BACKUP)

        private val keyBytes = byteArrayOf(KEY.length.toByte()) + KEY.encodeToByteArray()
        private val altKeyBytes = byteArrayOf(ALT_KEY.length.toByte()) + ALT_KEY.encodeToByteArray()

        private val journalHeader =
            byteArrayOf(0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, JOURNAL_VERSION, 0x00, 0x00, 0x00, 0x01)
        private val emptyJournal = journalHeader

        private val journalWithDirty =
            byteArrayOf(*journalHeader, JournalEntry.DIRTY, *keyBytes, JournalEntry.DIRTY, *altKeyBytes)

        private val journalWithDirtyData = JournalData(
            cleanEntriesKeys = emptyList(),
            dirtyEntriesKeys = listOf(KEY, ALT_KEY),
            redundantEntriesCount = 2,
        )

        private val journalWithAdd =
            byteArrayOf(*journalHeader, JournalEntry.DIRTY, *keyBytes, JournalEntry.CLEAN, *keyBytes)

        private val journalWithAddData = JournalData(
            cleanEntriesKeys = listOf(KEY),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 1,
        )

        private val journalWithMixedAdd =
            byteArrayOf(
                *journalWithDirty,
                JournalEntry.CLEAN, *keyBytes,
                JournalEntry.CLEAN, *altKeyBytes,
            )
        private val journalWithMixedAddData = JournalData(
            cleanEntriesKeys = listOf(KEY, ALT_KEY),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 2,
        )

        private val journalWithAddAndRemove =
            byteArrayOf(
                *journalWithAdd,
                JournalEntry.DIRTY, *keyBytes,
                JournalEntry.REMOVE, *keyBytes,
            )
        private val journalWithAddAndRemoveData = JournalData(
            cleanEntriesKeys = emptyList(),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 4,
        )

        private val journalWithAddAndReAdd =
            byteArrayOf(
                *journalWithAdd,
                JournalEntry.DIRTY, *keyBytes,
                JournalEntry.CLEAN, *keyBytes,
            )
        private val journalWithAddAndReAddData = JournalData(
            cleanEntriesKeys = listOf(KEY),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 3,
        )

        private val journalWithAddAndDirtyAdd =
            byteArrayOf(
                *journalWithAdd,
                JournalEntry.DIRTY, *keyBytes,
            )
        private val journalWithAddAndDirtyAddData = JournalData(
            cleanEntriesKeys = listOf(KEY),
            dirtyEntriesKeys = listOf(KEY),
            redundantEntriesCount = 2,
        )

        private val journalWithAddAndCancelledAdd =
            byteArrayOf(
                *journalWithAdd,
                JournalEntry.DIRTY, *keyBytes,
                JournalEntry.CANCEL, *keyBytes,
            )

        private val journalWithAddAndCancelledAddData = JournalData(
            cleanEntriesKeys = listOf(KEY),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 3,
        )
    }
}
