package com.mayakapps.lrucache.io

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import platform.Foundation.*
import kotlin.test.*

@Suppress("CAST_NEVER_SUCCEEDS")
class FileStreamsTests {

    // 'FileInputStream' Tests

    @Test
    fun testInitFileInputStreamNonExistent() {
        shouldThrow<FileNotFoundException> {
            FileInputStream(tempFile)
        }
    }

    @Test
    fun testReadByte() {
        (TEST_STRING as NSString).writeToFile(tempFile, atomically = false)

        FileInputStream(tempFile).use { inputStream ->
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
        (TEST_STRING as NSString).writeToFile(tempFile, atomically = false)

        val buffer = ByteArray(6)
        FileInputStream(tempFile).use { inputStream ->
            assertSoftly {
                inputStream.read(buffer) shouldBeExactly 6
                buffer shouldBe testBytesP1

                inputStream.read(buffer) shouldBeExactly 6
                buffer shouldBe testBytesP2

                inputStream.read(buffer) shouldBeExactly -1
            }
        }
    }

    // 'FileOutputStream' Tests

    @Test
    fun testInitFileOutputStreamNonExistent() {
        FileOutputStream(tempFile).close()
        NSFileManager.defaultManager.fileExistsAtPath(tempFile) shouldBe true
    }

    @Test
    fun testWriteByte() {
        FileOutputStream(tempFile).use { outputStream ->
            for (byte in testBytes) {
                outputStream.write(byte.toInt())
            }
        }

        NSString.stringWithContentsOfFile(tempFile) shouldBe TEST_STRING
    }

    @Test
    fun testWriteBytes() {
        FileOutputStream(tempFile).use { outputStream ->
            outputStream.write(testBytesP1)
            outputStream.write(testBytesP2)
        }

        NSString.stringWithContentsOfFile(tempFile) shouldBe TEST_STRING
    }

    @Test
    fun testAppendBytes() {
        (TEST_STRING_P1 as NSString).writeToFile(tempFile, atomically = false)

        FileOutputStream(tempFile, append = true).use { outputStream ->
            outputStream.write(testBytesP2)
        }

        NSString.stringWithContentsOfFile(tempFile) shouldBe TEST_STRING
    }

    @Test
    fun testOverwriteBytes() {
        (TEST_STRING_P1 as NSString).writeToFile(tempFile, atomically = false)

        FileOutputStream(tempFile, append = false).use { outputStream ->
            outputStream.write(testBytesP2)
        }

        NSString.stringWithContentsOfFile(tempFile) shouldBe TEST_STRING_P2
    }

    // Test Callbacks

    @BeforeTest
    fun beforeTest() {
        fileManager.createDirectoryAtPath(rootDirectory, true, null, null)
    }

    @AfterTest
    fun afterTest() {
        fileManager.removeItemAtPath(rootDirectory, null)
    }

    @ThreadLocal
    companion object {
        private lateinit var fileManager: NSFileManager
        private lateinit var rootDirectory: String

        private lateinit var tempFile: String
        private lateinit var tempDirectory: String

        private lateinit var testBytes: ByteArray
        private lateinit var testBytesP1: ByteArray
        private lateinit var testBytesP2: ByteArray

        @BeforeClass
        fun beforeClass() {
            fileManager = NSFileManager.defaultManager

            val tempDirName = NSProcessInfo.processInfo.globallyUniqueString
            rootDirectory = NSString.pathWithComponents(listOf(NSTemporaryDirectory(), tempDirName))

            tempFile = NSString.pathWithComponents(listOf(rootDirectory, "file.tmp"))
            tempDirectory = NSString.pathWithComponents(listOf(rootDirectory, "directory"))

            testBytes = TEST_STRING.encodeToByteArray()
            testBytesP1 = TEST_STRING_P1.encodeToByteArray()
            testBytesP2 = TEST_STRING_P2.encodeToByteArray()
        }

        private const val TEST_STRING_P1 = "Hello,"
        private const val TEST_STRING_P2 = " World"
        private const val TEST_STRING = TEST_STRING_P1 + TEST_STRING_P2
    }
}