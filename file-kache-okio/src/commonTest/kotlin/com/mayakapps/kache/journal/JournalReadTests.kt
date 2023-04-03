package com.mayakapps.kache.journal

import com.mayakapps.kache.combineResults
import com.mayakapps.kache.named
import com.mayakapps.kache.nullableUse
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.Matcher
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import okio.*
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test

class JournalReadTests {

    @Test
    fun testValidateHeaderEmptyStream() {
        shouldThrow<JournalInvalidHeaderException> {
            JournalReader(Buffer()).use { it.validateHeader() }
        }
    }

    @Test
    fun testValidateHeaderIncorrectFile() {
        // Text file containing "Text File"
        val bytes = byteArrayOf(0x54, 0x65, 0x78, 0x74, 0x20, 0x46, 0x69, 0x6C, 0x65)
        shouldThrow<JournalInvalidHeaderException> {
            JournalReader(Buffer().apply { write(bytes) }).use { it.validateHeader() }
        }
    }

    @Test
    fun testValidateHeaderIncorrectVersion() {
        val bytes = byteArrayOf(
            0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, 0x00,
            0x01, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        shouldThrow<JournalInvalidHeaderException> {
            JournalReader(Buffer().apply { write(bytes) }).use { it.validateHeader() }
        }
    }

    @Test
    fun testValidateHeaderValidHeader() {
        val bytes = byteArrayOf(
            0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, 0x01,
        )

        shouldNotThrow<JournalInvalidHeaderException> {
            JournalReader(Buffer().apply { write(bytes) }).use { it.validateHeader() }
        }
    }

    @Test
    fun testReadEntryEmptyJournal() {
        val bytes = byteArrayOf(
            0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, 0x01,
        )

        JournalReader(Buffer().apply { write(bytes) }).nullableUse {
            it.validateHeader()
            it.readEntry()
        } shouldBe null
    }

    @Test
    fun testReadSingleDirtyOperation() {
        val bytes = byteArrayOf(
            0x01, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val readResult = JournalReader(Buffer().apply { write(bytes) }).use { it.readEntry() }
        readResult shouldMatch JournalEntry.Dirty("TestKey")
    }

    @Test
    fun testReadSingleCleanOperation() {
        val bytes = byteArrayOf(
            0x02, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val readResult = JournalReader(Buffer().apply { write(bytes) }).use { it.readEntry() }
        readResult shouldMatch JournalEntry.Clean("TestKey")
    }

    @Test
    fun testReadSingleRemoveOperation() {
        val bytes = byteArrayOf(
            0x03, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val readResult = JournalReader(Buffer().apply { write(bytes) }).use { it.readEntry() }
        readResult shouldMatch JournalEntry.Remove("TestKey")
    }

    @Test
    fun testReadInvalidOperation() {
        val bytes = byteArrayOf(
            0x04, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        shouldThrow<JournalInvalidOpcodeException> {
            JournalReader(Buffer().apply { write(bytes) }).use { it.readEntry() }
        }
    }

    @Test
    fun testReadOperationWithoutKey() {
        val bytes = byteArrayOf(0x04)

        shouldThrow<EOFException> {
            JournalReader(Buffer().apply { write(bytes) }).use { it.readEntry() }
        }
    }

    @Test
    fun testReadOperationWithTruncatedKey() {
        val bytes = byteArrayOf(
            0x04, 0x07, 0x54, 0x65, 0x73, 0x74,
        )

        shouldThrow<EOFException> {
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
        source.wasClosed shouldBe true
    }

    @Test
    fun testReadNonExistingJournal() {
        val fileSystem = FakeFileSystem()

        fileSystem.readJournalIfExists(directory) shouldBe null
    }

    @Test
    fun testReadSimpleJournal() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAdd) }

        fileSystem.readJournalIfExists(directory) shouldMatch journalWithAddData
    }

    @Test
    fun testReadSimpleBackupJournal() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(backupJournalFile).buffer().use { it.write(journalWithAdd) }

        assertSoftly {
            fileSystem.readJournalIfExists(directory) shouldMatch journalWithAddData
            fileSystem.exists(journalFile) shouldBe true
            fileSystem.exists(backupJournalFile) shouldBe false
        }
    }

    @Test
    fun testReadJournalAlongWithTempAndBackupJournal() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAdd) }
        fileSystem.sink(tempJournalFile).buffer().use { it.write(emptyJournal) }
        fileSystem.sink(backupJournalFile).buffer().use { it.write(emptyJournal) }

        assertSoftly {
            fileSystem.readJournalIfExists(directory) shouldMatch journalWithAddData
            fileSystem.exists(journalFile) shouldBe true
            fileSystem.exists(tempJournalFile) shouldBe false
            fileSystem.exists(backupJournalFile) shouldBe false
        }
    }

    @Test
    fun testReadJournalWithDirtyAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithDirty) }

        fileSystem.readJournalIfExists(directory) shouldMatch journalWithDirtyData
    }

    @Test
    fun testReadJournalWithAddingAndRemoving() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAddAndRemove) }

        fileSystem.readJournalIfExists(directory) shouldMatch journalWithAddAndRemoveData
    }

    @Test
    fun testReadJournalWithMixedAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithMixedAdd) }

        fileSystem.readJournalIfExists(directory) shouldMatch journalWithMixedAddData
    }

    @Test
    fun testReadJournalWithAddingAndReAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAddAndReAdd) }

        fileSystem.readJournalIfExists(directory) shouldMatch journalWithAddAndReAddData
    }

    @Test
    fun testReadJournalWithAddingAndDirtyAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAddAndDirtyAdd) }

        fileSystem.readJournalIfExists(directory) shouldMatch journalWithAddAndDirtyAddData
    }

    @Test
    fun testReadJournalWithAddingAndCancelledAdding() {
        val fileSystem = FakeFileSystem()
        fileSystem.createDirectories(directory)
        fileSystem.sink(journalFile).buffer().use { it.write(journalWithAddAndCancelledAdd) }

        fileSystem.readJournalIfExists(directory) shouldMatch journalWithAddAndCancelledAddData
    }

    // Matchers

    private infix fun JournalData?.shouldMatch(reference: JournalData?) =
        this should matchData(reference)

    private fun matchData(reference: JournalData?) = Matcher<JournalData?> { value ->
        combineResults(
            "Journal data should match reference",
            "Journal data shouldn't match reference",
            beNull().invert().test(value),
            reference!!.cleanEntriesKeys.named("cleanEntriesKeys").test(value!!.cleanEntriesKeys),
            reference.dirtyEntriesKeys.named("dirtyEntriesKeys").test(value.dirtyEntriesKeys),
            reference.redundantEntriesCount.named("redundantEntriesCount").test(value.redundantEntriesCount),
        )
    }

    private infix fun JournalEntry?.shouldMatch(reference: JournalEntry?) =
        this should matchEntry(reference)

    private fun matchEntry(reference: JournalEntry?) = Matcher<JournalEntry?> { value ->
        combineResults(
            "Read result should match reference",
            "Read result shouldn't match reference",
            beNull().invert().test(value),
            reference!!.opcode.named("opcode").test(value!!.opcode),
            reference.key.named("key").test(value.key),
        )
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

        private val journalHeader = byteArrayOf(0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, 0x01)
        private val emptyJournal = journalHeader

        private val journalWithDirty = byteArrayOf(*journalHeader, 0x01, *keyBytes, 0x01, *altKeyBytes)
        private val journalWithDirtyData = JournalData(
            cleanEntriesKeys = emptyList(),
            dirtyEntriesKeys = listOf(KEY, ALT_KEY),
            redundantEntriesCount = 2,
        )

        private val journalWithAdd = byteArrayOf(*journalHeader, 0x01, *keyBytes, 0x02, *keyBytes)
        private val journalWithAddData = JournalData(
            cleanEntriesKeys = listOf(KEY),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 1,
        )

        private val journalWithMixedAdd = byteArrayOf(*journalWithDirty, 0x02, *keyBytes, 0x02, *altKeyBytes)
        private val journalWithMixedAddData = JournalData(
            cleanEntriesKeys = listOf(KEY, ALT_KEY),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 2,
        )

        private val journalWithAddAndRemove = byteArrayOf(*journalWithAdd, 0x01, *keyBytes, 0x03, *keyBytes)
        private val journalWithAddAndRemoveData = JournalData(
            cleanEntriesKeys = emptyList(),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 4,
        )

        private val journalWithAddAndReAdd = byteArrayOf(*journalWithAdd, 0x01, *keyBytes, 0x02, *keyBytes)
        private val journalWithAddAndReAddData = JournalData(
            cleanEntriesKeys = listOf(KEY),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 3,
        )

        private val journalWithAddAndDirtyAdd = byteArrayOf(*journalWithAdd, 0x01, *keyBytes)
        private val journalWithAddAndDirtyAddData = JournalData(
            cleanEntriesKeys = listOf(KEY),
            dirtyEntriesKeys = listOf(KEY),
            redundantEntriesCount = 2,
        )

        // TODO: should be different from adding and removing
        private val journalWithAddAndCancelledAdd = byteArrayOf(*journalWithAdd, 0x01, *keyBytes, 0x03, *keyBytes)
        private val journalWithAddAndCancelledAddData = JournalData(
            cleanEntriesKeys = listOf(KEY),
            dirtyEntriesKeys = emptyList(),
            redundantEntriesCount = 3,
        )
    }
}