/*
 * Copyright 2023-2024 MayakaApps
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mayakapps.kache

import com.mayakapps.kache.journal.FILES_DIR
import com.mayakapps.kache.journal.JOURNAL_FILE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.use
import kotlin.test.*

class OkioFileKacheTest {

    @Test
    fun getKeys() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)

        // Empty
        assertContentEquals(emptyList(), kache.getKeys())

        // Non-empty
        kache.put(fileSystem, KEY_1, VAL_1)
        kache.put(fileSystem, KEY_2, VAL_2)
        assertContentEquals(listOf(KEY_1, KEY_2), kache.getKeys())

        // Under-creation
        @Suppress("DeferredResultUnused")
        kache.putAsync(fileSystem, KEY_3, VAL_3)
        assertContentEquals(listOf(KEY_1, KEY_2), kache.getKeys())

        // Re-open
        kache.close()
        val kache2 = testOkioFileKache(fileSystem)
        assertContentEquals(listOf(KEY_1, KEY_2), kache2.getKeys())
    }

    @Test
    fun getUnderCreationKeys() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)

        // Empty
        assertContentEquals(emptyList(), kache.getUnderCreationKeys())

        // Non-empty
        kache.put(fileSystem, KEY_1, VAL_1)
        assertContentEquals(emptyList(), kache.getUnderCreationKeys())

        // Under-creation
        @Suppress("DeferredResultUnused")
        kache.putAsync(fileSystem, KEY_2, VAL_2)
        assertContentEquals(listOf(KEY_2), kache.getUnderCreationKeys())

        // Re-open
        kache.close()
        val kache2 = testOkioFileKache(fileSystem)
        assertContentEquals(emptyList(), kache2.getUnderCreationKeys())
    }

    @Test
    fun getAllKeys() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)

        // Empty
        assertEquals(KacheKeys(emptySet(), emptySet()), kache.getAllKeys())

        // Non-empty
        kache.put(fileSystem, KEY_1, VAL_1)
        kache.put(fileSystem, KEY_2, VAL_2)
        assertEquals(KacheKeys(setOf(KEY_1, KEY_2), emptySet()), kache.getAllKeys())

        // Under-creation
        @Suppress("DeferredResultUnused")
        kache.putAsync(fileSystem, KEY_3, VAL_3)
        assertEquals(KacheKeys(setOf(KEY_1, KEY_2), setOf(KEY_3)), kache.getAllKeys())

        // Re-open
        kache.close()
        val kache2 = testOkioFileKache(fileSystem)
        assertEquals(KacheKeys(setOf(KEY_1, KEY_2), emptySet()), kache2.getAllKeys())
    }

    @Test
    fun get() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)

        // Non-existing
        assertTrue(kache.get(KEY_1) == null)

        // Existing
        kache.put(fileSystem, KEY_1, VAL_1)
        val firstCachePath = kache.get(KEY_1)
        fileSystem.assertPathContentEquals(VAL_1, firstCachePath)

        // Under-creation
        @Suppress("DeferredResultUnused")
        kache.putAsync(fileSystem, KEY_2, VAL_2)
        val twoCachePath = kache.get(KEY_2)
        fileSystem.assertPathContentEquals(VAL_2, twoCachePath)
    }

    @Test
    fun getIfAvailable() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)

        // Non-existing
        assertTrue(kache.getIfAvailable(KEY_1) == null)

        // Existing
        kache.put(fileSystem, KEY_1, VAL_1)
        val firstCachePath = kache.getIfAvailable(KEY_1)
        fileSystem.assertPathContentEquals(VAL_1, firstCachePath)

        // Under-creation
        val deferred = kache.putAsync(fileSystem, KEY_2, VAL_2)
        assertNull(kache.getIfAvailable(KEY_2))
        deferred.await()
        val twoCachePath = kache.getIfAvailable(KEY_2)
        fileSystem.assertPathContentEquals(VAL_2, twoCachePath)
    }

    @Test
    fun getOrPut() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)


        // getOrPut a new entry with successful creation
        val firstCachePath = kache.getOrPut(KEY_1) {
            fileSystem.sink(it).buffer().use { it.writeUtf8(VAL_1) }
            true
        }
        fileSystem.assertPathContentEquals(VAL_1, firstCachePath)

        // getOrPut a new entry with failed creation
        var capturedSecondCachePath: Path? = null
        val secondCachePath = kache.getOrPut(KEY_2) {
            capturedSecondCachePath = it
            fileSystem.sink(it).buffer().use { it.writeUtf8(VAL_2) }
            false
        }
        assertNull(secondCachePath)
        assertFalse(fileSystem.exists(capturedSecondCachePath!!))

        // getOrPut an existing with successful creation
        val firstCachePath1 = kache.getOrPut(KEY_1) {
            fileSystem.sink(it).buffer().use { it.writeUtf8(VAL_2) }
            true
        }
        assertEquals(firstCachePath, firstCachePath1)
        fileSystem.assertPathContentEquals(VAL_1, firstCachePath1)

        // getOrPut an existing entry with failed creation
        val firstCachePath2 = kache.getOrPut(KEY_1) {
            fileSystem.sink(it).buffer().use { it.writeUtf8(VAL_2) }
            false
        }
        assertEquals(firstCachePath, firstCachePath2)
        fileSystem.assertPathContentEquals(VAL_1, firstCachePath2)

        // getOrPut an under-creation entry with successful creation
        val putAsyncDeferred = kache.putAsync(fileSystem, KEY_3, VAL_3)
        val thirdCachePath = kache.getOrPut(KEY_3) {
            fileSystem.sink(it).buffer().use { it.writeUtf8(VAL_2) }
            true
        }
        fileSystem.assertPathContentEquals(VAL_3, thirdCachePath)
        try {
            putAsyncDeferred.await()
        } catch (e: CancellationException) {
            fail("Deferred should not be cancelled")
        }

        // getOrPut an under-creation entry with failed creation
        @Suppress("DeferredResultUnused")
        kache.putAsync(KEY_4) { cachePath ->
            delay(60_000)
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_3) }
            false
        }
        val fourthCachePath = kache.getOrPut(KEY_4) {
            fileSystem.sink(it).buffer().use { it.writeUtf8(VAL_4) }
            true
        }
        fileSystem.assertPathContentEquals(VAL_4, fourthCachePath)
    }

    @Test
    fun put() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)

        // put a new entry with successful creation
        val firstCachePath = kache.put(KEY_1) { cachePath ->
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_1) }
            true
        }
        fileSystem.assertPathContentEquals(VAL_1, firstCachePath)

        // Kache entries are: [(KEY_1: VAL_1)]

        // put a new entry with failed creation
        var capturedSecondCachePath: Path? = null
        val secondCachePath = kache.put(KEY_2) { cachePath ->
            capturedSecondCachePath = cachePath
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_2) }
            false
        }
        assertNull(secondCachePath)
        assertFalse(fileSystem.exists(capturedSecondCachePath!!))

        // Kache entries are: [(KEY_1: VAL_1)]

        // put an existing entry with successful creation
        val firstCachePath1 = kache.put(KEY_1) { cachePath ->
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_2) }
            true
        }
        fileSystem.assertPathContentEquals(VAL_2, firstCachePath)
        assertEquals(firstCachePath, firstCachePath1)

        // Kache entries are: [(KEY_1: VAL_2)]

        // put an existing entry with failed creation
        val firstCachePath2 = kache.put(KEY_1) { cachePath ->
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_3) }
            false
        }
        fileSystem.assertPathContentEquals(VAL_2, firstCachePath)
        assertNull(firstCachePath2)

        // Kache entries are: [(KEY_1: VAL_2)]

        // put an under-creation entry with successful creation
        val putAsyncDeferred = kache.putAsync(fileSystem, KEY_3, VAL_3)
        val thirdCachePath = kache.put(KEY_3) { cachePath ->
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_4) }
            true
        }
        fileSystem.assertPathContentEquals(VAL_4, thirdCachePath)
        assertFailsWith<CancellationException> { putAsyncDeferred.await() }

        // Kache entries are: [(KEY_1: VAL_2), (KEY_3: VAL_4)]

        // put an under-creation entry with failed creation
        @Suppress("DeferredResultUnused")
        kache.putAsync(KEY_4) { cachePath ->
            delay(60_000)
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_3) }
            false
        }
        val fourthCachePath = kache.put(KEY_4) { cachePath ->
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_4) }
            true
        }
        fileSystem.assertPathContentEquals(VAL_4, fourthCachePath)
    }

    @Test
    fun putAsync() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)

        // putAsync a new entry with successful creation
        val putAsyncDeferred = kache.putAsync(KEY_1) { cachePath ->
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_1) }
            true
        }
        val firstCachePath = putAsyncDeferred.await()
        fileSystem.assertPathContentEquals(VAL_1, firstCachePath)

        // Kache entries are: [(KEY_1: VAL_1)]

        // putAsync a new entry with failed creation
        var capturedSecondCachePath: Path? = null
        val putAsyncDeferred2 = kache.putAsync(KEY_2) { cachePath ->
            capturedSecondCachePath = cachePath
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_2) }
            false
        }
        val secondCachePath = putAsyncDeferred2.await()
        assertNull(secondCachePath)
        assertFalse(fileSystem.exists(capturedSecondCachePath!!))

        // Kache entries are: [(KEY_1: VAL_1)]

        // putAsync an existing entry with successful creation
        val putAsyncDeferred3 = kache.putAsync(KEY_1) { cachePath ->
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_2) }
            true
        }
        val firstCachePath1 = putAsyncDeferred3.await()
        fileSystem.assertPathContentEquals(VAL_2, firstCachePath)
        assertEquals(firstCachePath, firstCachePath1)

        // Kache entries are: [(KEY_1: VAL_2)]

        // putAsync an existing entry with failed creation
        val putAsyncDeferred4 = kache.putAsync(KEY_1) { cachePath ->
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_3) }
            false
        }
        val firstCachePath2 = putAsyncDeferred4.await()
        fileSystem.assertPathContentEquals(VAL_2, firstCachePath)
        assertNull(firstCachePath2)

        // Kache entries are: [(KEY_1: VAL_2)]

        // putAsync an under-creation entry with successful creation
        val putAsyncDeferred5 = kache.putAsync(fileSystem, KEY_3, VAL_3)
        val thirdCachePath = kache.putAsync(KEY_3) { cachePath ->
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_4) }
            true
        }.await()
        fileSystem.assertPathContentEquals(VAL_4, thirdCachePath)
        assertFailsWith<CancellationException> { putAsyncDeferred5.await() }

        // Kache entries are: [(KEY_1: VAL_2), (KEY_3: VAL_4)]

        // putAsync an under-creation entry with failed creation
        @Suppress("DeferredResultUnused")
        kache.putAsync(KEY_4) { cachePath ->
            delay(60_000)
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_3) }
            false
        }
        val fourthCachePath = kache.putAsync(KEY_4) { cachePath ->
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_4) }
            true
        }.await()
        fileSystem.assertPathContentEquals(VAL_4, fourthCachePath)
    }

    @Test
    fun remove() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)

        // Remove an existing entry
        kache.put(fileSystem, KEY_1, VAL_1)
        kache.put(fileSystem, KEY_2, VAL_2)
        kache.remove(KEY_1)
        assertNull(kache.get(KEY_1))
        fileSystem.assertPathContentEquals(VAL_2, kache.get(KEY_2))

        // Remove an under-creation entry
        @Suppress("DeferredResultUnused")
        kache.putAsync(KEY_3) { cachePath ->
            delay(60_000)
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(VAL_3) }
            true
        }
        kache.remove(KEY_3)
        assertNull(kache.get(KEY_3))
        fileSystem.assertPathContentEquals(VAL_2, kache.get(KEY_2))

        // Remove a non-existing entry
        kache.remove(KEY_4)
        fileSystem.assertPathContentEquals(VAL_2, kache.get(KEY_2))
    }

    @Test
    fun clear() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)

        kache.put(fileSystem, KEY_1, VAL_1)
        kache.put(fileSystem, KEY_2, VAL_2)
        println(kache.get(KEY_1))
        println(kache.get(KEY_2))
        println(fileSystem.list(filesDirectory))
        kache.clear()

        // The cache should be empty
        println(kache.get(KEY_1))
        println(kache.get(KEY_2))
        println(fileSystem.list(filesDirectory))
        assertContentEquals(emptyList(), fileSystem.list(filesDirectory))
        assertNull(kache.get(KEY_1))
        assertNull(kache.get(KEY_2))
    }

    @Test
    fun removeAllUnderCreation() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)

        kache.put(fileSystem, KEY_1, VAL_1)
        kache.put(fileSystem, KEY_2, VAL_2)

        var capturedCachePath: Path? = null
        val deferred = kache.putAsync(KEY_3) { cachePath ->
            capturedCachePath = cachePath
            println("capturedCachePath: $capturedCachePath")
            fileSystem.sink(cachePath).buffer().use {
                delay(60_000)
                it.writeUtf8(VAL_3)
            }
            true
        }
        delay(1000)
        kache.removeAllUnderCreation()

        // The deferred should be cancelled
        assertFailsWith<CancellationException> { deferred.await() }

        // The cache should contain only the existing entries
        fileSystem.assertPathContentEquals(VAL_1, kache.get(KEY_1))
        fileSystem.assertPathContentEquals(VAL_2, kache.get(KEY_2))
        assertNull(kache.get(KEY_3))

        // The under-creation entry should be removed
        assertFalse(fileSystem.exists(capturedCachePath!!))
    }

    @Test
    fun close() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)

        kache.put(fileSystem, KEY_1, VAL_1)
        kache.close()

        // The journal should be closed
        try {
            fileSystem.checkNoOpenFiles()
        } catch (e: IllegalStateException) {
            fail(e.message)
        }
    }

    @Test
    fun rebuildingJournal() = runTest {
        val fileSystem = FakeFileSystem()
        val kache = testOkioFileKache(fileSystem)

        // Each failed put() call will add two redundant entries to the journal
        repeat((OkioFileKache.REDUNDANT_ENTRIES_THRESHOLD / 2) - 1) {
            kache.put(KEY_1) { false }
        }

        // The journal should not be rebuilt yet
        assertTrue((fileSystem.metadata(journalFile).size ?: 0) > 1024)

        kache.put(KEY_1) { false }

        // Make sure that the journal was rebuilt by checking that its size is less than 1 KB (a small size)
        assertTrue((fileSystem.metadata(journalFile).size ?: 0) < 1024)
    }

    @Test
    fun reopen() = runTest {
        // The cache should keep the entries after re-open
        val contentFileSystem = FakeFileSystem()
        createThreeElementOkioFileKacheWithAccess(contentFileSystem, KacheStrategy.LRU)
        val contentKache = testOkioFileKache(contentFileSystem)
        contentFileSystem.assertPathContentEquals(VAL_1, contentKache.get(KEY_1))
        contentFileSystem.assertPathContentEquals(VAL_2, contentKache.get(KEY_2))
        contentFileSystem.assertPathContentEquals(VAL_3, contentKache.get(KEY_3))

        // LRU: The cache should keep the order of the entries
        val lruFileSystem = FakeFileSystem()
        createThreeElementOkioFileKacheWithAccess(lruFileSystem, KacheStrategy.LRU)
        val lruKache = testOkioFileKache(lruFileSystem, maxSize = 17L) { strategy = KacheStrategy.LRU }
        lruKache.put(lruFileSystem, KEY_4, VAL_4)
        assertNull(lruKache.get(KEY_3))

        // FIFO: The cache should keep the order of the entries
        val fifoFileSystem = FakeFileSystem()
        createThreeElementOkioFileKacheWithAccess(fifoFileSystem, KacheStrategy.FIFO)
        val fifoKache = testOkioFileKache(fifoFileSystem, maxSize = 17L) { strategy = KacheStrategy.FIFO }
        fifoKache.put(fifoFileSystem, KEY_4, VAL_4)
        assertNull(fifoKache.get(KEY_1))

        // The cache should be empty after re-open if the journal is corrupted
        val corruptedFileSystem = FakeFileSystem()
        createThreeElementOkioFileKacheWithAccess(corruptedFileSystem, KacheStrategy.LRU)
        corruptedFileSystem.write(journalFile) { writeUtf8("corrupted") }
        val corruptedKache = testOkioFileKache(corruptedFileSystem)
        assertNull(corruptedKache.get(KEY_1))
        assertNull(corruptedKache.get(KEY_2))
        assertNull(corruptedKache.get(KEY_3))
        assertEquals(0, corruptedFileSystem.list(filesDirectory).size)
    }

    private suspend fun TestScope.testOkioFileKache(
        fileSystem: FileSystem = FakeFileSystem(),
        maxSize: Long = MAX_SIZE,
        configuration: OkioFileKache.Configuration.() -> Unit = {}
    ) = OkioFileKache(directory, maxSize) {
        this.fileSystem = fileSystem
        this.creationScope = this@testOkioFileKache
        configuration()
    }

    private suspend fun OkioFileKache.put(fileSystem: FileSystem, key: String, value: String): Path? {
        return put(key) { cachePath ->
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(value) }
            true
        }
    }

    private suspend fun OkioFileKache.putAsync(fileSystem: FileSystem, key: String, value: String): Deferred<Path?> {
        return putAsync(key) { cachePath ->
            delay(60_000)
            fileSystem.sink(cachePath).buffer().use { it.writeUtf8(value) }
            true
        }
    }


    /**
     * Puts 3 elements into the kache and gets the first two of them. The fourth should be added after re-open
     * This way the state of the kache is as follows:
     * - The least-recently-used element is [KEY_3] with [VAL_3]
     * - The most-recently-used element is [KEY_4] with [VAL_4]
     * - The first-in element is [KEY_1] with [VAL_1]
     * - The last-in element is [KEY_4] with [VAL_4]
     */
    private suspend fun TestScope.createThreeElementOkioFileKacheWithAccess(
        fileSystem: FileSystem,
        strategy: KacheStrategy,
    ) {
        val kache = testOkioFileKache(fileSystem) {
            this.strategy = strategy
        }

        kache.put(fileSystem, KEY_1, VAL_1)
        kache.put(fileSystem, KEY_2, VAL_2)
        kache.put(fileSystem, KEY_3, VAL_3)
        kache.get(KEY_1)
        kache.get(KEY_2)
        kache.close()
    }

    private fun FileSystem.assertPathContentEquals(content: String, path: Path?) {
        assertNotNull(path)
        assertTrue(exists(path))
        assertEquals(content, source(path).buffer().use { it.readUtf8() })
    }

    companion object {
        private const val MAX_SIZE = 1024L

        private const val KEY_1 = "one"
        private const val VAL_1 = "first"
        private const val KEY_2 = "two"
        private const val VAL_2 = "second"
        private const val KEY_3 = "three"
        private const val VAL_3 = "third"
        private const val KEY_4 = "four"
        private const val VAL_4 = "fourth"

        private val directory = "/cache/".toPath()
        private val filesDirectory = directory.resolve(FILES_DIR)
        private val journalFile = directory.resolve(JOURNAL_FILE)
    }
}
