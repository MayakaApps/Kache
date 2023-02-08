package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.combineResults
import com.mayakapps.lrucache.io.ByteArrayInputStream
import com.mayakapps.lrucache.io.use
import com.mayakapps.lrucache.named
import io.kotest.matchers.Matcher
import io.kotest.matchers.should
import kotlin.test.Test

class JournalReaderTests {

    @Test
    fun testReadEmptyStream() {
        val bytes = ByteArray(0)
        val readResult = JournalReader(ByteArrayInputStream(bytes)).use { it.readJournal() }
        readResult shouldMatch JournalReader.Result(isCorrupted = true, cleanKeys = emptyList(), opsCount = 0)
    }

    @Test
    fun testReadIncorrectFile() {
        // Text file containing "Text File"
        val bytes = byteArrayOf(0x54, 0x65, 0x78, 0x74, 0x20, 0x46, 0x69, 0x6C, 0x65)
        val readResult = JournalReader(ByteArrayInputStream(bytes)).use { it.readJournal() }
        readResult shouldMatch JournalReader.Result(isCorrupted = true, cleanKeys = emptyList(), opsCount = 0)
    }

    @Test
    fun testReadIncorrectVersion() {
        val bytes = byteArrayOf(
            0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, 0x00,
            0x01, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val readResult = JournalReader(ByteArrayInputStream(bytes)).use { it.readJournal() }
        readResult shouldMatch JournalReader.Result(isCorrupted = true, cleanKeys = emptyList(), opsCount = 0)
    }

    @Test
    fun testReadEmptyJournal() {
        val bytes = byteArrayOf(
            0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, 0x01,
        )

        val readResult = JournalReader(ByteArrayInputStream(bytes)).use { it.readJournal() }
        readResult shouldMatch JournalReader.Result(isCorrupted = false, cleanKeys = emptyList(), opsCount = 0)
    }

    @Test
    fun testReadSingleDirtyOperation() {
        val bytes = byteArrayOf(
            0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, 0x01,
            0x01, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val readResult = JournalReader(ByteArrayInputStream(bytes)).use { it.readJournal() }
        readResult shouldMatch JournalReader.Result(isCorrupted = false, cleanKeys = emptyList(), opsCount = 1)
    }

    @Test
    fun testReadSingleCleanOperation() {
        val bytes = byteArrayOf(
            0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, 0x01,
            0x02, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val readResult = JournalReader(ByteArrayInputStream(bytes)).use { it.readJournal() }
        readResult shouldMatch JournalReader.Result(isCorrupted = false, cleanKeys = listOf("TestKey"), opsCount = 1)
    }

    @Test
    fun testReadAddedOperation() {
        val bytes = byteArrayOf(
            0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, 0x01,
            0x01, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
            0x02, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val readResult = JournalReader(ByteArrayInputStream(bytes)).use { it.readJournal() }
        readResult shouldMatch JournalReader.Result(isCorrupted = false, cleanKeys = listOf("TestKey"), opsCount = 2)
    }

    @Test
    fun testReadAddedRemovedOperation() {
        val bytes = byteArrayOf(
            0x4A, 0x4F, 0x55, 0x52, 0x4E, 0x41, 0x4C, 0x01,
            0x01, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
            0x02, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
            0x01, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
            0x03, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val readResult = JournalReader(ByteArrayInputStream(bytes)).use { it.readJournal() }
        readResult shouldMatch JournalReader.Result(isCorrupted = false, cleanKeys = emptyList(), opsCount = 4)
    }

    private infix fun JournalReader.Result.shouldMatch(reference: JournalReader.Result) =
        this should matchResult(reference)

    private fun matchResult(reference: JournalReader.Result) = Matcher<JournalReader.Result> { value ->
        combineResults(
            "Read result should match reference",
            "Read result shouldn't match reference",
            reference.isCorrupted.named("isCorrupted").test(value.isCorrupted),
            reference.cleanKeys.named("cleanKeys").test(value.cleanKeys),
            reference.opsCount.named("opsCount").test(value.opsCount),
        )
    }
}