package com.mayakapps.lrucache.io

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import platform.Foundation.*
import kotlin.test.*

@Suppress("CAST_NEVER_SUCCEEDS")
class ByteArrayStreamsTests {

    // 'ByteArrayInputStream' Tests

    @Test
    fun testReadByteFromEmpty() {
        ByteArrayInputStream(ByteArray(0)).use { inputStream ->
            inputStream.read() shouldBeExactly -1
        }
    }

    @Test
    fun testReadByte() {
        ByteArrayInputStream(testBytes).use { inputStream ->
            assertSoftly {
                for (byte in testBytes) {
                    inputStream.read() shouldBeExactly byte.toInt()
                }

                inputStream.read() shouldBeExactly -1
            }
        }
    }

    @Test
    fun testReadBytes() {
        val buffer = ByteArray(6)
        ByteArrayInputStream(testBytes).use { inputStream ->
            assertSoftly {
                inputStream.read(buffer) shouldBeExactly 6
                buffer shouldBe testBytesP1

                inputStream.read(buffer) shouldBeExactly 6
                buffer shouldBe testBytesP2

                inputStream.read(buffer) shouldBeExactly -1
            }
        }
    }

    // 'ByteArrayOutputStream' Tests

    @Test
    fun testEmptyByteArrayOutputStream() {
        val outputStream = ByteArrayOutputStream()
        outputStream.close()
        outputStream.toByteArray() shouldBe ByteArray(0)
    }

    @Test
    fun testWriteByte() {
        val outputStream = ByteArrayOutputStream()

        outputStream.use {
            for (byte in testBytes) {
                outputStream.write(byte.toInt())
            }
        }

        outputStream.toByteArray() shouldBe testBytes
    }

    @Test
    fun testWriteBytes() {
        ByteArrayOutputStream().use { outputStream ->
            outputStream.write(testBytesP1)
            outputStream.write(testBytesP2)

            outputStream.toByteArray() shouldBe testBytes
        }
    }

    @ThreadLocal
    companion object {
        private lateinit var testBytes: ByteArray
        private lateinit var testBytesP1: ByteArray
        private lateinit var testBytesP2: ByteArray

        @BeforeClass
        fun beforeClass() {
            testBytes = TEST_STRING.encodeToByteArray()
            testBytesP1 = TEST_STRING_P1.encodeToByteArray()
            testBytesP2 = TEST_STRING_P2.encodeToByteArray()
        }

        private const val TEST_STRING_P1 = "Hello,"
        private const val TEST_STRING_P2 = " World"
        private const val TEST_STRING = TEST_STRING_P1 + TEST_STRING_P2
    }
}