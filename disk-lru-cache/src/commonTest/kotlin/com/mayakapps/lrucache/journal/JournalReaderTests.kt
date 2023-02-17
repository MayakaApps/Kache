package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.combineResults
import com.mayakapps.lrucache.named
import com.mayakapps.lrucache.nullableUse
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.Matcher
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import okio.Buffer
import okio.use
import kotlin.test.Test

class JournalReaderTests {

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

    private infix fun JournalData.shouldMatch(reference: JournalData) =
        this should matchData(reference)

    private fun matchData(reference: JournalData) = Matcher<JournalData> { value ->
        combineResults(
            "Journal data should match reference",
            "Journal data shouldn't match reference",
            reference.cleanEntriesKeys.named("cleanEntriesKeys").test(value.cleanEntriesKeys),
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
}