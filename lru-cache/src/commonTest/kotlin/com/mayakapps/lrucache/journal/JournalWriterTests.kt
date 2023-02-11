package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.ByteArrayOutputStream
import com.mayakapps.lrucache.io.OutputStream
import com.mayakapps.lrucache.io.use
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JournalWriterTests {

    @Test
    fun testWriteHeader() {
        val outputStream = ByteArrayOutputStream()
        JournalWriter(outputStream).use { it.writeHeader() }
        outputStream.toByteArray() shouldBe headerBytes
    }

    @Test
    fun testWriteDirty() {
        val bytes = byteArrayOf(
            0x01, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val outputStream = ByteArrayOutputStream()
        JournalWriter(outputStream).use { it.writeDirty(KEY) }
        outputStream.toByteArray() shouldBe bytes
    }

    @Test
    fun testWriteClean() {
        val bytes = byteArrayOf(
            0x02, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val outputStream = ByteArrayOutputStream()
        JournalWriter(outputStream).use { it.writeClean(KEY) }
        outputStream.toByteArray() shouldBe bytes
    }

    @Test
    fun testWriteRemove() {
        val bytes = byteArrayOf(
            0x03, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val outputStream = ByteArrayOutputStream()
        JournalWriter(outputStream).use { it.writeRemove(KEY) }
        outputStream.toByteArray() shouldBe bytes
    }

    @Test
    fun testWriteAll() {
        val bytes = byteArrayOf(
            0x02, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
            0x01, 0x0A, 0x41, 0x6C, 0x74, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val outputStream = ByteArrayOutputStream()
        JournalWriter(outputStream).use { it.writeAll(listOf(KEY), listOf(ALT_KEY)) }
        outputStream.toByteArray() shouldBe bytes
    }

    @Test
    fun testClose() {
        val outputStream = object : OutputStream() {
            var wasClosed = false

            override fun close() {
                wasClosed = true
            }

            override fun write(byte: Int) {}
        }

        JournalWriter(outputStream).close()
        outputStream.wasClosed shouldBe true
    }

    companion object {
        private const val KEY = "TestKey"
        private const val ALT_KEY = "AltTestKey"
        private val headerBytes = JOURNAL_MAGIC.encodeToByteArray() + JOURNAL_VERSION.toByte()
    }
}