package com.mayakapps.lrucache.io

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import platform.Foundation.*
import kotlin.math.min
import kotlin.test.*

@Suppress("CAST_NEVER_SUCCEEDS")
class BufferedStreamsTests {

    // 'BufferedInputStream' Tests

    @Test
    fun testInitBufferedInputStream() {
        val inputStream = TestInputStream()
        BufferedInputStream(inputStream, BUFFER_SIZE).use {
            inputStream.reads shouldHaveSize 0
        }
    }

    @Test
    fun testCloseBufferedInputStream() {
        val inputStream = TestInputStream()
        BufferedInputStream(inputStream, BUFFER_SIZE).close()
        inputStream.wasClosed shouldBe true
    }

    @Test
    fun testReadSingleByte(): Unit = assertSoftly {
        val inputStream = TestInputStream()

        BufferedInputStream(inputStream, BUFFER_SIZE).use { it.read() } shouldBeExactly 0x7F
        inputStream.reads shouldContainExactly listOf(BUFFER_SIZE)
    }

    @Test
    fun testReadByteByByte(): Unit = assertSoftly {
        val inputStream = TestInputStream()

        BufferedInputStream(inputStream, BUFFER_SIZE).use { bufferedStream ->
            repeat(BUFFER_SIZE * 3) {
                bufferedStream.read() shouldBeExactly 0x7F
            }
        }

        inputStream.reads shouldContainExactly listOf(BUFFER_SIZE, BUFFER_SIZE, BUFFER_SIZE)
    }

    @Test
    fun testReadExactlyBufferSize(): Unit = assertSoftly {
        val inputStream = TestInputStream()
        val buffer = ByteArray(BUFFER_SIZE)

        BufferedInputStream(inputStream, BUFFER_SIZE).use { it.read(buffer) } shouldBeExactly BUFFER_SIZE
        buffer shouldBe ByteArray(BUFFER_SIZE) { 0x7F }
        inputStream.reads shouldContainExactly listOf(BUFFER_SIZE)
    }

    @Test
    fun testReadMoreThanBufferSize(): Unit = assertSoftly {
        val inputStream = TestInputStream()
        val buffer = ByteArray(BUFFER_SIZE + 16)

        BufferedInputStream(inputStream, BUFFER_SIZE).use { it.read(buffer) } shouldBeExactly buffer.size
        buffer shouldBe ByteArray(buffer.size) { 0x7F }
        inputStream.reads shouldContainExactly listOf(buffer.size)
    }

    @Test
    fun testReadMoreThanRemaining(): Unit = assertSoftly {
        val inputStream = TestInputStream()
        val buffer = ByteArray(BUFFER_SIZE)

        val availableBytes = BUFFER_SIZE - 16
        inputStream.remaining = availableBytes

        BufferedInputStream(inputStream, BUFFER_SIZE).use { it.read(buffer) } shouldBeExactly availableBytes
        buffer shouldBe ByteArray(buffer.size) { i -> if (i < availableBytes) 0x7F else 0x00 }
        inputStream.reads shouldContainExactly listOf(availableBytes)
    }

    private class TestInputStream : InputStream() {

        val reads = mutableListOf<Int>()
        var remaining = -1
        var wasClosed = false

        override fun read(): Int = defaultRead(this)

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val finalLength = if (remaining == -1) length
            else {
                val finalLength = min(remaining, length)
                remaining -= finalLength
                finalLength
            }

            reads += finalLength
            buffer.fill(0x7F, offset, offset + finalLength)
            return finalLength
        }

        override fun close() {
            wasClosed = true
        }
    }

    // 'BufferedOutputStream' Tests

    @Test
    fun testInitBufferedOutputStream() {
        val outputStream = TestOutputStream()
        BufferedOutputStream(outputStream, BUFFER_SIZE).use {
            outputStream.writes shouldHaveSize 0
        }
    }

    @Test
    fun testCloseBufferedOutputStream() {
        val outputStream = TestOutputStream()
        BufferedOutputStream(outputStream, BUFFER_SIZE).close()
        outputStream.wasClosed shouldBe true
    }

    @Test
    fun testWriteSingleByte(): Unit = assertSoftly {
        val outputStream = TestOutputStream()

        BufferedOutputStream(outputStream, BUFFER_SIZE).use {
            it.write(0x7F)
            outputStream.writes shouldHaveSize 0
        }

        outputStream.writes shouldContainExactly listOf(byteArrayOf(0x7F))
    }

    @Test
    fun testWriteByteByByte(): Unit = assertSoftly {
        val bytes = ByteArray(BUFFER_SIZE) { 0x7F }
        val outputStream = TestOutputStream()

        BufferedOutputStream(outputStream, BUFFER_SIZE).use { bufferedStream ->
            repeat(BUFFER_SIZE * 3) {
                bufferedStream.write(0x7F)
            }
        }

        outputStream.writes shouldContainExactly listOf(bytes, bytes, bytes)
    }

    @Test
    fun testWriteExactlyBufferSize(): Unit = assertSoftly {
        val bytes = ByteArray(BUFFER_SIZE) { 0x7F }

        val outputStream = TestOutputStream()
        BufferedOutputStream(outputStream, BUFFER_SIZE).use {
            it.write(bytes)
        }

        outputStream.writes shouldContainExactly listOf(bytes)
    }

    @Test
    fun testWriteMoreThanBufferSize(): Unit = assertSoftly {
        val bytes = ByteArray(BUFFER_SIZE + 16) { 0x7F }

        val outputStream = TestOutputStream()
        BufferedOutputStream(outputStream, BUFFER_SIZE).use {
            it.write(bytes)
        }

        outputStream.writes shouldContainExactly listOf(bytes)
    }

    @Test
    fun testFlush(): Unit = assertSoftly {
        val bytes = ByteArray(BUFFER_SIZE - 10) { 0x7F }
        val outputStream = TestOutputStream()

        BufferedOutputStream(outputStream, BUFFER_SIZE).use { bufferedStream ->
            bufferedStream.write(bytes)
            outputStream.writes shouldHaveSize 0
            bufferedStream.flush()
            outputStream.writes shouldContainExactly listOf(bytes)
        }
    }

    private class TestOutputStream : OutputStream() {

        val writes = mutableListOf<ByteArray>()
        var wasClosed = false

        override fun write(byte: Int) = defaultWrite(this, byte)

        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            writes += buffer.copyOfRange(offset, offset + length)
        }

        override fun close() {
            wasClosed = true
        }
    }

    companion object {
        private const val BUFFER_SIZE = 64
    }
}