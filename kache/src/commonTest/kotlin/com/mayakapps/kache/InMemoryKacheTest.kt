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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class InMemoryKacheTest {

    @Test
    fun getKeys() = runTest {
        val kache = testInMemoryKache()

        // Empty
        assertEquals(emptySet(), kache.getKeys())

        // Non-empty
        kache.put(KEY_1, VAL_1)
        kache.put(KEY_2, VAL_2)
        assertEquals(setOf(KEY_1, KEY_2), kache.getKeys())
    }

    @Test
    fun getUnderCreationKeys() = runTest {
        val kache = testInMemoryKache()

        // Empty
        assertEquals(emptySet(), kache.getUnderCreationKeys())

        // Non-empty
        @Suppress("DeferredResultUnused")
        kache.putAsync(KEY_1) { VAL_1 }
        @Suppress("DeferredResultUnused")
        kache.putAsync(KEY_2) { VAL_2 }
        assertEquals(setOf(KEY_1, KEY_2), kache.getUnderCreationKeys())
    }

    @Test
    fun getAllKeys() = runTest {
        val kache = testInMemoryKache()

        // Empty
        assertEquals(KacheKeys(emptySet(), emptySet()), kache.getAllKeys())

        // Non-empty
        kache.put(KEY_1, VAL_1)
        @Suppress("DeferredResultUnused")
        kache.putAsync(KEY_2) { VAL_2 }
        assertEquals(KacheKeys(setOf(KEY_1), setOf(KEY_2)), kache.getAllKeys())
    }

    @Test
    fun getOrDefault() = runTest {
        val kache = testInMemoryKache()

        // Non-existing
        assertEquals(VAL_1, kache.getOrDefault(KEY_1, VAL_1))

        // Existing
        kache.put(KEY_1, VAL_1)
        assertEquals(VAL_1, kache.getOrDefault(KEY_1, VAL_3))

        // Creating
        @Suppress("DeferredResultUnused")
        kache.putAsync(KEY_2) { VAL_2 }
        assertEquals(VAL_2, kache.getOrDefault(KEY_2, VAL_3))
    }

    @Test
    fun get() = runTest {
        val kache = testInMemoryKache()

        // Non-existing
        assertNull(kache.get(KEY_1))

        // Existing
        kache.put(KEY_1, VAL_1)
        assertEquals(VAL_1, kache.get(KEY_1))

        // Creating
        @Suppress("DeferredResultUnused")
        kache.putAsync(KEY_2) { VAL_2 }
        assertEquals(VAL_2, kache.get(KEY_2))
    }

    @Test
    fun getIfAvailableOrDefault() = runTest {
        val kache = testInMemoryKache()

        // Non-existing
        assertEquals(VAL_1, kache.getIfAvailableOrDefault(KEY_1, VAL_1))

        // Existing
        kache.put(KEY_1, VAL_1)
        assertEquals(VAL_1, kache.getIfAvailableOrDefault(KEY_1, VAL_2))

        // Creating
        @Suppress("DeferredResultUnused")
        kache.putAsync(KEY_2) { VAL_2 }
        assertEquals(VAL_3, kache.getIfAvailableOrDefault(KEY_2, VAL_3))
    }

    @Test
    fun getIfAvailable() = runTest {
        val kache = testInMemoryKache()

        // Non-existing
        assertNull(kache.getIfAvailable(KEY_1))

        // Existing
        kache.put(KEY_1, VAL_1)
        assertEquals(VAL_1, kache.getIfAvailable(KEY_1))

        // Creating
        @Suppress("DeferredResultUnused")
        kache.putAsync(KEY_2) { VAL_2 }
        assertNull(kache.getIfAvailable(KEY_2))
    }

    @Test
    fun getOrPut() = runTest {
        val kache = testInMemoryKache()

        // getOrPut a new entry with successful creation
        assertEquals(VAL_1, kache.getOrPut(KEY_1) { VAL_1 })

        // getOrPut a new entry with failed creation
        assertNull(kache.getOrPut(KEY_2) { null })

        // getOrPut an existing with successful creation
        assertEquals(VAL_1, kache.getOrPut(KEY_1) { VAL_2 })

        // getOrPut an existing entry with failed creation
        assertEquals(VAL_1, kache.getOrPut(KEY_1) { null })

        // getOrPut an under-creation entry with successful creation
        val deferred1 = kache.putAsync(KEY_2) { VAL_2 }
        assertEquals(VAL_2, kache.getOrPut(KEY_2) { VAL_3 })
        try {
            assertEquals(VAL_2, deferred1.await())
        } catch (_: CancellationException) {
            fail("Deferred should not be cancelled")
        }
        assertEquals(VAL_2, kache.getIfAvailable(KEY_2))

        // getOrPut an under-creation entry with failed creation
        val deferred2 = kache.putAsync(KEY_3) { VAL_3 }
        assertEquals(VAL_3, kache.getOrPut(KEY_3) { null })
        try {
            assertEquals(VAL_3, deferred2.await())
        } catch (_: CancellationException) {
            fail("Deferred should not be cancelled")
        }
        assertEquals(VAL_3, kache.getIfAvailable(KEY_3))

        // getOrPut simultaneously with put operations
        // See issue #50 (https://github.com/MayakaApps/Kache/issues/50) for more details
        @Suppress("DeferredResultUnused")
        kache.putAsync(KEY_4) {
            delay(60_000)
            VAL_4
        }

        launch { kache.getOrPut(KEY_4) { VAL_4 } }
        delay(1)
        kache.put(KEY_5) { VAL_5 }

        @OptIn(ExperimentalCoroutinesApi::class)
        assertEquals(1L, currentTime)
    }

    @Test
    fun put() = runTest {
        val removalLogger = EntryRemovalLogger<String, Int>()
        val kache = testInMemoryKache(entryRemovalLogger = removalLogger)

        // put a new entry
        assertNull(kache.put(KEY_1, VAL_1))
        assertEquals(VAL_1, kache.getIfAvailable(KEY_1))
        assertEquals(0, removalLogger.getAndClearEvents().size)

        // put an existing entry
        assertEquals(VAL_1, kache.put(KEY_1, VAL_2))
        assertEquals(VAL_2, kache.getIfAvailable(KEY_1))
        assertContentEquals(
            listOf(EntryRemovalLogger.Event(false, KEY_1, VAL_1, VAL_2)),
            removalLogger.getAndClearEvents(),
        )

        // put an under-creation entry
        val deferred = kache.putAsync(KEY_3) { VAL_3 }
        assertNull(kache.put(KEY_3, VAL_4))
        assertFailsWith<CancellationException> { deferred.await() }
        assertEquals(VAL_4, kache.getIfAvailable(KEY_3))
        assertEquals(0, removalLogger.getAndClearEvents().size)

        // put a very big entry
        val tinyKache = testInMemoryKache(maxSize = 10) {
            sizeCalculator = { _, _ -> 20 }
        }
        tinyKache.put(KEY_1, VAL_1)
        assertEquals(0, tinyKache.size)
        assertNull(tinyKache.getIfAvailable(KEY_1))
    }

    /*
     * put with creation function tests
     */

    @Test
    fun putWithCreation() = runTest {
        val removalLogger = EntryRemovalLogger<String, Int>()
        val kache = testInMemoryKache(entryRemovalLogger = removalLogger)

        // put a new entry with successful creation
        assertEquals(VAL_1, kache.put(KEY_1) { VAL_1 })
        assertEquals(VAL_1, kache.getIfAvailable(KEY_1))
        assertEquals(0, removalLogger.getAndClearEvents().size)

        // Kache entries are: [(KEY_1: VAL_1)]

        // put a new entry with failed creation
        assertNull(kache.put(KEY_2) { null })
        assertNull(kache.getIfAvailable(KEY_2))

        // Kache entries are: [(KEY_1: VAL_1)]

        // put an existing entry with successful creation
        assertEquals(VAL_2, kache.put(KEY_1) { VAL_2 })
        assertEquals(VAL_2, kache.getIfAvailable(KEY_1))
        assertContentEquals(
            listOf(EntryRemovalLogger.Event(false, KEY_1, VAL_1, VAL_2)),
            removalLogger.getAndClearEvents(),
        )

        // Kache entries are: [(KEY_1: VAL_2)]

        // put an existing entry with failed creation
        assertNull(kache.put(KEY_1) { null })
        assertEquals(VAL_2, kache.getIfAvailable(KEY_1))
        assertEquals(0, removalLogger.getAndClearEvents().size)

        // Kache entries are: [(KEY_1: VAL_2)]

        // put an under-creation entry with successful creation
        val deferred1 = kache.putAsync(KEY_2) { VAL_2 }
        assertEquals(VAL_3, kache.put(KEY_2) { VAL_3 })
        assertFailsWith<CancellationException> { deferred1.await() }
        assertEquals(VAL_3, kache.getIfAvailable(KEY_2))
        assertEquals(0, removalLogger.getAndClearEvents().size)

        // Kache entries are: [(KEY_1: VAL_2), (KEY_2: VAL_3)]

        // put an under-creation entry with failed creation
        val deferred2 = kache.putAsync(KEY_3) { VAL_3 }
        assertNull(kache.put(KEY_3) { null })
        assertFailsWith<CancellationException> { deferred2.await() }
        assertNull(kache.getIfAvailable(KEY_3))
        assertEquals(0, removalLogger.getAndClearEvents().size)
    }

    /*
     * putAsync tests
     */

    @Test
    fun putAsync() = runTest {
        val removalLogger = EntryRemovalLogger<String, Int>()
        val kache = testInMemoryKache(entryRemovalLogger = removalLogger)

        // putAsync a new entry with successful creation
        val deferred1 = kache.putAsync(KEY_1) { VAL_1 }
        assertNull(kache.getIfAvailable(KEY_1))
        assertEquals(VAL_1, deferred1.await())
        assertEquals(VAL_1, kache.getIfAvailable(KEY_1))
        assertEquals(0, removalLogger.getAndClearEvents().size)

        // Kache entries are: [(KEY_1: VAL_1)]

        // putAsync a new entry with failed creation
        val deferred2 = kache.putAsync(KEY_2) { null }
        assertNull(kache.getIfAvailable(KEY_2))
        assertNull(deferred2.await())
        assertNull(kache.getIfAvailable(KEY_2))

        // Kache entries are: [(KEY_1: VAL_1)]

        // putAsync an existing entry with successful creation
        val deferred3 = kache.putAsync(KEY_1) { VAL_2 }
        assertEquals(VAL_2, deferred3.await())
        assertEquals(VAL_2, kache.getIfAvailable(KEY_1))
        assertContentEquals(
            listOf(EntryRemovalLogger.Event(false, KEY_1, VAL_1, VAL_2)),
            removalLogger.getAndClearEvents(),
        )

        // Kache entries are: [(KEY_1: VAL_2)]

        // putAsync an existing entry with failed creation
        val deferred4 = kache.putAsync(KEY_1) { null }
        assertEquals(VAL_2, kache.getIfAvailable(KEY_1))
        assertNull(deferred4.await())
        assertEquals(VAL_2, kache.getIfAvailable(KEY_1))
        assertEquals(0, removalLogger.getAndClearEvents().size)

        // Kache entries are: [(KEY_1: VAL_2)]

        // putAsync an under-creation entry with successful creation
        val deferred5 = kache.putAsync(KEY_2) { VAL_2 }
        val deferred6 = kache.putAsync(KEY_2) { VAL_3 }
        assertFailsWith<CancellationException> { deferred5.await() }
        assertEquals(VAL_3, deferred6.await())
        assertEquals(VAL_3, kache.getIfAvailable(KEY_2))
        assertEquals(0, removalLogger.getAndClearEvents().size)

        // Kache entries are: [(KEY_1: VAL_2), (KEY_2: VAL_3)]

        // putAsync an under-creation entry with failed creation
        val deferred7 = kache.putAsync(KEY_3) { VAL_3 }
        val deferred8 = kache.putAsync(KEY_3) { null }
        assertFailsWith<CancellationException> { deferred7.await() }
        assertNull(deferred8.await())
        assertNull(kache.getIfAvailable(KEY_3))
    }

    /*
     * Nesting Tests
     */

    @Test
    fun nestedPut() = runTest {
        val kache = testInMemoryKache()

        // getIfAvailable a key inside its put operation
        kache.put(KEY_1, VAL_1)
        kache.put(KEY_1) {
            assertEquals(VAL_1, kache.getIfAvailable(KEY_1))
            VAL_2
        }
        assertEquals(VAL_2, kache.getIfAvailable(KEY_1))

        // Kache entries are: [(KEY_1: VAL_2)]

        // getIfAvailable a key inside put operation of another key
        kache.put(KEY_2) {
            assertEquals(VAL_2, kache.getIfAvailable(KEY_1))
            VAL_2
        }
        assertEquals(VAL_2, kache.getIfAvailable(KEY_2))

        // Kache entries are: [(KEY_1: VAL_2), (KEY_2: VAL_2)]

        // get a key inside put operation of another key
        kache.put(KEY_3) {
            assertEquals(VAL_2, kache.get(KEY_1))
            VAL_3
        }
        assertEquals(VAL_3, kache.getIfAvailable(KEY_3))

        // Kache entries are: [(KEY_1: VAL_2), (KEY_2: VAL_2), (KEY_3: VAL_3)]

        // remove a key inside its put operation
        kache.put(KEY_3) {
            kache.remove(KEY_3)
            VAL_4
        }
        assertEquals(VAL_4, kache.getIfAvailable(KEY_3))

        // Kache entries are: [(KEY_1: VAL_2), (KEY_2: VAL_2), (KEY_3: VAL_4)]

        // remove a key inside put operation of another key
        kache.put(KEY_1) {
            kache.remove(KEY_3)
            VAL_1
        }
        assertNull(kache.getIfAvailable(KEY_3))
        assertEquals(VAL_1, kache.getIfAvailable(KEY_1))

        // Kache entries are: [(KEY_1: VAL_1), (KEY_2: VAL_2)]

        // getOrPut a key inside put operation of another key
        kache.put(KEY_3) {
            assertEquals(VAL_1, kache.getOrPut(KEY_1) { VAL_2 })
            assertEquals(VAL_4, kache.getOrPut(KEY_4) { VAL_4 })
            VAL_3
        }
        assertEquals(VAL_1, kache.getIfAvailable(KEY_1))
        assertEquals(VAL_3, kache.getIfAvailable(KEY_3))
        assertEquals(VAL_4, kache.getIfAvailable(KEY_4))

        // Kache entries are: [(KEY_1: VAL_1), (KEY_2: VAL_2), (KEY_3: VAL_3), (KEY_4: VAL_4)]

        // put a key inside its put operation
        kache.put(KEY_1) {
            kache.put(KEY_1, VAL_3)
            VAL_2
        }
        assertEquals(VAL_2, kache.getIfAvailable(KEY_1))

        // Kache entries are: [(KEY_1: VAL_2), (KEY_2: VAL_2), (KEY_3: VAL_3), (KEY_4: VAL_4)]

        // put a key inside put operation of another key
        kache.put(KEY_5) {
            kache.put(KEY_1, VAL_1)
            VAL_5
        }
        assertEquals(VAL_1, kache.getIfAvailable(KEY_1))
        assertEquals(VAL_5, kache.getIfAvailable(KEY_5))
    }

    @Test
    fun remove() = runTest {
        val removalLogger = EntryRemovalLogger<String, Int>()
        val kache = testInMemoryKache(entryRemovalLogger = removalLogger)

        // Non-existing
        assertNull(kache.remove(KEY_1))
        assertEquals(0, removalLogger.getAndClearEvents().size)

        // Existing
        kache.put(KEY_1, VAL_1)
        assertEquals(VAL_1, kache.remove(KEY_1))
        assertNull(kache.getIfAvailable(KEY_1))
        assertContentEquals(
            listOf(EntryRemovalLogger.Event(false, KEY_1, VAL_1, null)),
            removalLogger.getAndClearEvents(),
        )

        // Creating
        val deferred = kache.putAsync(KEY_1) { VAL_1 }
        assertNull(kache.remove(KEY_1))
        assertFailsWith<CancellationException> { deferred.await() }
        assertNull(kache.get(KEY_1))
        assertEquals(0, removalLogger.getAndClearEvents().size)
    }

    @Test
    fun clear() = runTest {
        val removalLogger = EntryRemovalLogger<String, Int>()
        val kache = testInMemoryKache(entryRemovalLogger = removalLogger)

        // Existing
        kache.put(KEY_1, VAL_1)
        kache.put(KEY_2, VAL_2)
        kache.clear()
        assertEquals(0, kache.size)
        assertNull(kache.getIfAvailable(KEY_1))
        assertNull(kache.getIfAvailable(KEY_2))
        assertContentEquals(
            listOf(
                EntryRemovalLogger.Event(false, KEY_1, VAL_1, null),
                EntryRemovalLogger.Event(false, KEY_2, VAL_2, null),
            ),
            removalLogger.getAndClearEvents(),
        )

        // Creating
        val deferred1 = kache.putAsync(KEY_1) { VAL_1 }
        val deferred2 = kache.putAsync(KEY_2) { VAL_2 }
        kache.clear()
        assertEquals(0, kache.size)
        assertFailsWith<CancellationException> { deferred1.await() }
        assertFailsWith<CancellationException> { deferred2.await() }
        assertNull(kache.get(KEY_1))
        assertNull(kache.get(KEY_2))
        assertEquals(0, removalLogger.getAndClearEvents().size)
    }

    @Test
    fun removeAllUnderCreation() = runTest {
        val removalLogger = EntryRemovalLogger<String, Int>()
        val kache = testInMemoryKache(entryRemovalLogger = removalLogger)

        val deferred1 = kache.putAsync(KEY_1) { VAL_1 }
        val deferred2 = kache.putAsync(KEY_2) { VAL_2 }
        kache.removeAllUnderCreation()

        // deferred1 and deferred2 should be cancelled
        assertFailsWith<CancellationException> { deferred1.await() }
        assertFailsWith<CancellationException> { deferred2.await() }

        // KEY_1 and KEY_2 should not be cached
        assertNull(kache.get(KEY_1))
        assertNull(kache.get(KEY_2))

        // onEntryRemoved should not be called
        assertEquals(0, removalLogger.getAndClearEvents().size)
    }

    @Test
    fun autoResizeOnAddThenTrim() = runTest {
        val removalLogger = EntryRemovalLogger<String, Int>()

        // LRU
        val lruKache = testInMemoryKache(entryRemovalLogger = removalLogger) {
            strategy = KacheStrategy.LRU
            expireAfterWriteDuration = 60_000.milliseconds
        }
        lruKache.putEightElementsWithAccess()
        lruKache.trimToSize(5)
        assertEquals(5, lruKache.size)
        assertContentEquals(listOf(KEY_8, KEY_1, KEY_2, KEY_4, KEY_5), lruKache.getKeys())
        assertContentEquals(
            listOf(
                EntryRemovalLogger.Event(true, KEY_3, VAL_3, null),
                EntryRemovalLogger.Event(true, KEY_6, VAL_6, null),
                EntryRemovalLogger.Event(true, KEY_7, VAL_7, null),
            ),
            removalLogger.getAndClearEvents(),
        )
    }

    @Test
    fun trimToSize() = runTest {
        val removalLogger = EntryRemovalLogger<String, Int>()

        // LRU
        val lruKache = testInMemoryKache(entryRemovalLogger = removalLogger) {
            strategy = KacheStrategy.LRU
        }
        lruKache.putFourElementsWithAccess()
        lruKache.trimToSize(3)
        assertEquals(3, lruKache.size)
        assertContentEquals(listOf(KEY_4, KEY_1, KEY_2), lruKache.getKeys())
        assertContentEquals(
            listOf(EntryRemovalLogger.Event(true, KEY_3, VAL_3, null)),
            removalLogger.getAndClearEvents(),
        )

        // MRU
        val mruKache = testInMemoryKache(entryRemovalLogger = removalLogger) {
            strategy = KacheStrategy.MRU
        }
        mruKache.putFourElementsWithAccess()
        mruKache.trimToSize(3)
        assertEquals(3, mruKache.size)
        assertContentEquals(listOf(KEY_1, KEY_4, KEY_3), mruKache.getKeys())
        assertContentEquals(
            listOf(EntryRemovalLogger.Event(true, KEY_2, VAL_2, null)),
            removalLogger.getAndClearEvents(),
        )

        // FIFO
        val fifoKache = testInMemoryKache(entryRemovalLogger = removalLogger) {
            strategy = KacheStrategy.FIFO
        }
        fifoKache.putFourElementsWithAccess()
        fifoKache.trimToSize(3)
        assertEquals(3, fifoKache.size)
        assertContentEquals(listOf(KEY_2, KEY_3, KEY_4), fifoKache.getKeys())
        assertContentEquals(
            listOf(EntryRemovalLogger.Event(true, KEY_1, VAL_1, null)),
            removalLogger.getAndClearEvents(),
        )

        // FILO
        val filoKache = testInMemoryKache(entryRemovalLogger = removalLogger) {
            strategy = KacheStrategy.FILO
        }
        filoKache.putFourElementsWithAccess()
        filoKache.trimToSize(3)
        assertEquals(3, filoKache.size)
        assertContentEquals(listOf(KEY_3, KEY_2, KEY_1), filoKache.getKeys())
        assertContentEquals(
            listOf(EntryRemovalLogger.Event(true, KEY_4, VAL_4, null)),
            removalLogger.getAndClearEvents(),
        )
    }

    @Test
    fun resize() = runTest {
        val removalLogger = EntryRemovalLogger<String, Int>()
        val kache = testInMemoryKache(entryRemovalLogger = removalLogger)

        // Empty
        kache.resize(1)
        assertEquals(0, kache.size)
        assertEquals(1, kache.maxSize)
        assertEquals(0, removalLogger.getAndClearEvents().size)

        // Non-empty
        kache.resize(10)
        kache.putFourElementsWithAccess()
        kache.resize(3)
        assertEquals(3, kache.size)
        assertEquals(3, kache.maxSize)
        assertEquals(1, removalLogger.getAndClearEvents().size)

        kache.clear()
        removalLogger.clear()

        // Test trigger
        kache.put(KEY_1, VAL_1)
        kache.put(KEY_2, VAL_2)
        kache.resize(1)
        assertContentEquals(
            listOf(EntryRemovalLogger.Event(true, KEY_1, VAL_1, null)),
            removalLogger.getAndClearEvents(),
        )
    }

    @Test
    fun eviction() = runTest {
        val removalLogger = EntryRemovalLogger<String, Int>()
        val kache = testInMemoryKache(maxSize = 2, entryRemovalLogger = removalLogger)

        kache.put(KEY_1, VAL_1)
        kache.put(KEY_2, VAL_2)
        assertEquals(2, kache.size)
        assertEquals(0, removalLogger.getAndClearEvents().size)

        kache.put(KEY_3, VAL_3)
        assertEquals(2, kache.size)
        assertEquals(1, removalLogger.getAndClearEvents().size)
    }

    @Test
    fun sizeCalculation() = runTest {
        val kache = testInMemoryKache(maxSize = 1L + VAL_1 + VAL_2) {
            sizeCalculator = { _, value -> value.toLong() }
        }

        kache.put(KEY_1, VAL_1)
        kache.put(KEY_2, VAL_2)

        assertEquals((VAL_1 + VAL_2).toLong(), kache.size)
    }

    @Test
    fun expireAfterWrite() = runTest {
        val timeSource = MsTimeSource()
        val kache = testInMemoryKache {
            this.timeSource = timeSource
            this.expireAfterWriteDuration = 1000.milliseconds
        }

        kache.put(KEY_1, VAL_1)
        assertEquals(VAL_1, kache.get(KEY_1))
        timeSource += 999
        assertEquals(VAL_1, kache.get(KEY_1))
        timeSource += 1
        assertNull(kache.get(KEY_1))
        assertEquals(0, kache.size)

        kache.put(KEY_1, VAL_1)
        timeSource += 1000
        assertEquals(VAL_2, kache.getOrPut(KEY_1) { VAL_2 })
    }

    @Test
    fun expireAfterAccess() = runTest {
        val timeSource = MsTimeSource()
        val kache = testInMemoryKache {
            this.timeSource = timeSource
            this.expireAfterAccessDuration = 1000.milliseconds
        }

        kache.put(KEY_1, VAL_1)
        assertEquals(VAL_1, kache.get(KEY_1))
        timeSource += 999
        assertEquals(VAL_1, kache.get(KEY_1))
        timeSource += 1
        assertEquals(VAL_1, kache.get(KEY_1))
        timeSource += 1000
        assertNull(kache.get(KEY_1))

        assertEquals(0, kache.size)
    }
}
