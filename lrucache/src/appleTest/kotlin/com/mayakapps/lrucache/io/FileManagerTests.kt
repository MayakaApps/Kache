package com.mayakapps.lrucache.io

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotlinx.cinterop.UnsafeNumber
import platform.Foundation.*
import kotlin.test.*

class FileManagerTests {

    // 'DefaultFileManager.exists()' Tests

    @Test
    fun testExistsExistentDirectory() {
        NSFileManager.defaultManager.createDirectoryAtPath(tempDirectory, true, null, null)
        DefaultFileManager.exists(tempDirectory) shouldBe true
    }

    @Test
    fun testExistsExistentFile() {
        NSFileManager.defaultManager.createFileAtPath(tempFile, null, null)
        DefaultFileManager.exists(tempFile) shouldBe true
    }

    @Test
    fun testExistsNonExistent() {
        DefaultFileManager.exists(tempFile) shouldBe false
    }

    // 'DefaultFileManager.isDirectory()' Tests

    @Test
    fun testIsDirectoryExistentDirectory() {
        NSFileManager.defaultManager.createDirectoryAtPath(tempDirectory, true, null, null)
        DefaultFileManager.isDirectory(tempDirectory) shouldBe true
    }

    @Test
    fun testIsDirectoryExistentFile() {
        NSFileManager.defaultManager.createFileAtPath(tempFile, null, null)
        DefaultFileManager.isDirectory(tempFile) shouldBe false
    }

    @Test
    fun testIsDirectoryNonExistent() {
        DefaultFileManager.isDirectory(tempFile) shouldBe false
    }

    // 'DefaultFileManager.size()' Tests

    @Test
    fun testSizeFileWithContents() {
        NSFileManager.defaultManager.createFileAtPath(tempFile, testData, null)

        @OptIn(UnsafeNumber::class)
        DefaultFileManager.size(tempFile) shouldBeExactly testData.length.toLong()
    }

    @Test
    fun testSizeEmptyFile() {
        NSFileManager.defaultManager.createFileAtPath(tempFile, null, null)
        DefaultFileManager.size(tempFile) shouldBeExactly 0
    }

    @Test
    fun testSizeNonExistent() {
        DefaultFileManager.size(tempFile) shouldBeExactly 0
    }

    // 'DefaultFileManager.listContent()' Tests

    @Test
    fun testListContentDirectoryWithContents() {
        NSFileManager.defaultManager.createDirectoryAtPath(tempDirectory, true, null, null)

        val subFile1 = NSString.pathWithComponents(listOf(tempDirectory, "file1"))
        NSFileManager.defaultManager.createFileAtPath(subFile1, null, null)

        val subDir = NSString.pathWithComponents(listOf(tempDirectory, "dir"))
        NSFileManager.defaultManager.createDirectoryAtPath(subDir, true, null, null)

        val subFile2 = NSString.pathWithComponents(listOf(subDir, "file2"))
        NSFileManager.defaultManager.createFileAtPath(subFile2, null, null)

        DefaultFileManager.listContent(tempDirectory)!! shouldContainAll listOf(subFile1, subDir)
    }

    @Test
    fun testListContentEmptyDirectory() {
        NSFileManager.defaultManager.createDirectoryAtPath(tempDirectory, true, null, null)
        DefaultFileManager.listContent(tempDirectory)!! shouldHaveSize 0
    }

    @Test
    fun testListContentExistentFile() {
        NSFileManager.defaultManager.createFileAtPath(tempFile, null, null)
        DefaultFileManager.listContent(tempFile) shouldBe null
    }

    @Test
    fun testListContentNonExistent() {
        DefaultFileManager.listContent(tempFile) shouldBe null
    }

    // 'DefaultFileManager.delete()' Tests

    @Test
    fun testDeleteExistentDirectory() {
        NSFileManager.defaultManager.createDirectoryAtPath(tempDirectory, true, null, null)
        assertSoftly {
            DefaultFileManager.delete(tempDirectory) shouldBe true
            NSFileManager.defaultManager.fileExistsAtPath(tempDirectory) shouldBe false
        }
    }

    @Test
    fun testDeleteExistentFile() {
        NSFileManager.defaultManager.createFileAtPath(tempFile, null, null)
        assertSoftly {
            DefaultFileManager.delete(tempFile) shouldBe true
            NSFileManager.defaultManager.fileExistsAtPath(tempFile) shouldBe false
        }
    }

    @Test
    fun testDeleteNonExistentFile() {
        DefaultFileManager.delete(tempFile) shouldBe false
    }

    // 'DefaultFileManager.deleteRecursively()' Tests

    @Test
    fun testDeleteRecursivelyDirectoryWithContents() {
        NSFileManager.defaultManager.createDirectoryAtPath(tempDirectory, true, null, null)

        val subFile1 = NSString.pathWithComponents(listOf(tempDirectory, "file1"))
        NSFileManager.defaultManager.createFileAtPath(subFile1, null, null)

        val subDir = NSString.pathWithComponents(listOf(tempDirectory, "dir"))
        NSFileManager.defaultManager.createDirectoryAtPath(subDir, true, null, null)

        val subFile2 = NSString.pathWithComponents(listOf(subDir, "file2"))
        NSFileManager.defaultManager.createFileAtPath(subFile2, null, null)

        assertSoftly {
            DefaultFileManager.deleteRecursively(tempDirectory) shouldBe true

            NSFileManager.defaultManager.fileExistsAtPath(tempDirectory) shouldBe false
            NSFileManager.defaultManager.fileExistsAtPath(subFile1) shouldBe false
            NSFileManager.defaultManager.fileExistsAtPath(subDir) shouldBe false
            NSFileManager.defaultManager.fileExistsAtPath(subFile2) shouldBe false
        }
    }

    @Test
    fun testDeleteRecursivelyEmptyDirectory() {
        NSFileManager.defaultManager.createDirectoryAtPath(tempDirectory, true, null, null)
        assertSoftly {
            DefaultFileManager.deleteRecursively(tempDirectory) shouldBe true
            NSFileManager.defaultManager.fileExistsAtPath(tempDirectory) shouldBe false
        }
    }

    @Test
    fun testDeleteRecursivelyExistentFile() {
        NSFileManager.defaultManager.createFileAtPath(tempFile, null, null)
        assertSoftly {
            DefaultFileManager.deleteRecursively(tempFile) shouldBe true
            NSFileManager.defaultManager.fileExistsAtPath(tempFile) shouldBe false
        }
    }

    @Test
    fun testDeleteRecursivelyNonExistentFile() {
        DefaultFileManager.deleteRecursively(tempFile) shouldBe false
    }

    // 'DefaultFileManager.renameTo()' Tests

    @Test
    fun testRenameExistentDirectoryToNonExistentDirectory() {
        NSFileManager.defaultManager.createDirectoryAtPath(tempDirectory, true, null, null)

        val subFile = NSString.pathWithComponents(listOf(tempDirectory, "file1"))
        NSFileManager.defaultManager.createFileAtPath(subFile, null, null)

        val renamedSubFile = NSString.pathWithComponents(listOf(tempDirectoryAnother, "file1"))

        assertSoftly {
            DefaultFileManager.renameTo(tempDirectory, tempDirectoryAnother) shouldBe true
            NSFileManager.defaultManager.fileExistsAtPath(tempDirectory) shouldBe false

            NSFileManager.defaultManager.fileExistsAtPath(tempDirectoryAnother) shouldBe true
            NSFileManager.defaultManager.fileExistsAtPath(renamedSubFile) shouldBe true
        }
    }

    @Test
    fun testRenameExistentDirectoryToExistentDirectory() {
        NSFileManager.defaultManager.createDirectoryAtPath(tempDirectory, true, null, null)
        NSFileManager.defaultManager.createDirectoryAtPath(tempDirectoryAnother, true, null, null)

        assertSoftly {
            DefaultFileManager.renameTo(tempDirectory, tempDirectoryAnother) shouldBe false
            NSFileManager.defaultManager.fileExistsAtPath(tempDirectory) shouldBe true
            NSFileManager.defaultManager.fileExistsAtPath(tempDirectoryAnother) shouldBe true
        }
    }

    @Test
    fun testRenameExistentFileToNonExistentFile() {
        NSFileManager.defaultManager.createFileAtPath(tempFile, null, null)

        assertSoftly {
            DefaultFileManager.renameTo(tempFile, tempFileAnother) shouldBe true
            NSFileManager.defaultManager.fileExistsAtPath(tempFile) shouldBe false
            NSFileManager.defaultManager.fileExistsAtPath(tempFileAnother) shouldBe true
        }
    }

    @Test
    fun testRenameExistentFileToExistentFile() {
        NSFileManager.defaultManager.createFileAtPath(tempFile, null, null)
        NSFileManager.defaultManager.createFileAtPath(tempFileAnother, null, null)

        assertSoftly {
            DefaultFileManager.renameTo(tempFile, tempFileAnother) shouldBe false
            NSFileManager.defaultManager.fileExistsAtPath(tempFile) shouldBe true
            NSFileManager.defaultManager.fileExistsAtPath(tempFileAnother) shouldBe true
        }
    }

    @Test
    fun testRenameNonExistentFile() {
        DefaultFileManager.renameTo(tempFile, tempFileAnother) shouldBe false
    }

    // 'DefaultFileManager.createDirectories()' Tests

    @Test
    fun testCreateDirectoriesExistentDirectory() {
        NSFileManager.defaultManager.createDirectoryAtPath(tempDirectory, true, null, null)
        DefaultFileManager.createDirectories(tempDirectory) shouldBe true
    }

    @Test
    fun testCreateDirectoriesExistentFile() {
        NSFileManager.defaultManager.createFileAtPath(tempFile, null, null)
        DefaultFileManager.createDirectories(tempFile) shouldBe false
    }

    @Test
    fun testCreateDirectoriesNonExistentDirectory() {
        val subSubDirectory = NSString.pathWithComponents(listOf(tempDirectory, "dir1", "dir2"))

        assertSoftly {
            DefaultFileManager.createDirectories(subSubDirectory) shouldBe true
            NSFileManager.defaultManager.fileExistsAtPath(subSubDirectory) shouldBe true
        }
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
        private lateinit var tempFileAnother: String

        private lateinit var tempDirectory: String
        private lateinit var tempDirectoryAnother: String

        private lateinit var testData: NSData

        @BeforeClass
        fun beforeClass() {
            fileManager = NSFileManager.defaultManager

            val tempDirName = NSProcessInfo.processInfo.globallyUniqueString
            rootDirectory = NSString.pathWithComponents(listOf(NSTemporaryDirectory(), tempDirName))

            tempFile = NSString.pathWithComponents(listOf(rootDirectory, "file1.tmp"))
            tempFileAnother = NSString.pathWithComponents(listOf(rootDirectory, "file2.tmp"))

            tempDirectory = NSString.pathWithComponents(listOf(rootDirectory, "directory1"))
            tempDirectoryAnother = NSString.pathWithComponents(listOf(rootDirectory, "directory2"))

            @OptIn(UnsafeNumber::class)
            @Suppress("CAST_NEVER_SUCCEEDS")
            testData = ("Hello, World" as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!
        }
    }
}