package com.mayakapps.kache.journal

import io.kotest.matchers.shouldBe
import okio.*
import kotlin.test.Test

class JournalWriteTests {

    @Test
    fun testWriteHeader() {
        val buffer = Buffer()
        JournalWriter(buffer).use { it.writeHeader() }
        buffer.readByteArray() shouldBe headerBytes
    }

    @Test
    fun testWriteDirty() {
        val bytes = byteArrayOf(
            0x01, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val buffer = Buffer()
        JournalWriter(buffer).use { it.writeDirty(KEY) }
        buffer.readByteArray() shouldBe bytes
    }

    @Test
    fun testWriteClean() {
        val bytes = byteArrayOf(
            0x02, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val buffer = Buffer()
        JournalWriter(buffer).use { it.writeClean(KEY) }
        buffer.readByteArray() shouldBe bytes
    }

    @Test
    fun testWriteRemove() {
        val bytes = byteArrayOf(
            0x03, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val buffer = Buffer()
        JournalWriter(buffer).use { it.writeRemove(KEY) }
        buffer.readByteArray() shouldBe bytes
    }

    @Test
    fun testWriteAll() {
        val bytes = byteArrayOf(
            0x02, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
            0x01, 0x0A, 0x41, 0x6C, 0x74, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val buffer = Buffer()
        JournalWriter(buffer).use { it.writeAll(listOf(KEY), listOf(ALT_KEY)) }
        buffer.readByteArray() shouldBe bytes
    }

    @Test
    fun testClose() {
        val sink = object : Sink {
            var wasClosed = false

            override fun close() {
                wasClosed = true
            }

            override fun write(source: Buffer, byteCount: Long) {}
            override fun timeout(): Timeout = Timeout.NONE
            override fun flush() {}
        }

        JournalWriter(sink.buffer()).close()
        sink.wasClosed shouldBe true
    }

    companion object {
        private const val KEY = "TestKey"
        private const val ALT_KEY = "AltTestKey"
        private val headerBytes = JOURNAL_MAGIC.encodeToByteArray() + JOURNAL_VERSION
    }
}