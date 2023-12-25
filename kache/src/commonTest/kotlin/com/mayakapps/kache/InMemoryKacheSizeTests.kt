/*
 * Copyright 2023 MayakaApps
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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryKacheSizeTests {

    /*
     * 'trimToSize' Tests
     */

    @Test
    fun testTrimToSizeEndSize() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        trimToSize(1)
        assertEquals(1, size)
        assertEquals(MAX_SIZE, maxSize)
    }

    @Test
    fun testTrimToSizeElementsForLRU() = runBasicInMemoryKacheTest(strategy = KacheStrategy.LRU) {
        prepareKache()
        trimToSize(3)

        assertEquals(VAL_1, get(KEY_1))
        assertEquals(VAL_2, get(KEY_2))
        assertNull(get(KEY_3))
        assertEquals(VAL_4, get(KEY_4))
    }

    @Test
    fun testTrimToSizeElementsForMRU() = runBasicInMemoryKacheTest(strategy = KacheStrategy.MRU) {
        prepareKache()
        trimToSize(3)
        assertEquals(VAL_1, get(KEY_1))
        assertNull(get(KEY_2))
        assertEquals(VAL_3, get(KEY_3))
        assertEquals(VAL_4, get(KEY_4))
    }

    @Test
    fun testTrimToSizeElementsForFIFO() = runBasicInMemoryKacheTest(strategy = KacheStrategy.FIFO) {
        prepareKache()
        trimToSize(3)
        assertNull(get(KEY_1))
        assertEquals(VAL_2, get(KEY_2))
        assertEquals(VAL_3, get(KEY_3))
        assertEquals(VAL_4, get(KEY_4))
    }

    @Test
    fun testTrimToSizeElementsForFILO() = runBasicInMemoryKacheTest(strategy = KacheStrategy.FILO) {
        prepareKache()
        trimToSize(3)
        assertEquals(VAL_1, get(KEY_1))
        assertEquals(VAL_2, get(KEY_2))
        assertEquals(VAL_3, get(KEY_3))
        assertNull(get(KEY_4))
    }

    @Test
    fun testTrimToSizeTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        prepareKache()
        trimToSize(3)

        assertEquals(1, removedEntries.size)
        removedEntries.firstOrNull()?.run {
            assertTrue(evicted)
            assertEquals(KEY_3, key)
            assertEquals(VAL_3, oldValue)
            assertNull(newValue)
        }
    }

    /*
     * 'resize' Tests
     */

    @Test
    fun testResizeEndSize() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        resize(1)
        assertEquals(1, size)
        assertEquals(1, maxSize)
    }

    @Test
    fun testResizeElements() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        resize(1)
        assertNull(get(KEY_1))
        assertEquals(VAL_2, get(KEY_2))
    }

    @Test
    fun testResizeTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        resize(1)

        assertEquals(1, removedEntries.size)
        removedEntries.firstOrNull()?.run {
            assertTrue(evicted)
            assertEquals(KEY_1, key)
            assertEquals(VAL_1, oldValue)
            assertNull(newValue)
        }
    }

    /*
     * Eviction Tests
     */

    @Test
    fun testEviction() = runBasicInMemoryKacheRemoveListenerTest(maxSize = 1) { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)

        assertEquals(1, removedEntries.size)
        removedEntries.firstOrNull()?.run {
            assertTrue(evicted)
            assertEquals(KEY_1, key)
            assertEquals(VAL_1, oldValue)
            assertNull(newValue)
        }
    }

    @Test
    fun testEvictionSameAsMaxSize() = runBasicInMemoryKacheRemoveListenerTest(maxSize = 2) { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)

        assertEquals(2, size)
        assertEquals(0, removedEntries.size)
    }

    /*
     * Size Calculator Tests
     */

    @Test
    fun testSizeCalculator() = runBasicInMemoryKacheTest(
        maxSize = 1L + VAL_1 + VAL_2,
        sizeCalculator = { _, value -> value.toLong() },
    ) {
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)

        assertEquals((VAL_1 + VAL_2).toLong(), size)
    }


    /*
     * Helpers
     */

    /**
     * Puts 4 elements into the kache and gets 2 of them. This way the state of the kache is as follows:
     * - The least-recently-used element is [KEY_3] with [VAL_3]
     * - The most-recently-used element is [KEY_2] with [VAL_2]
     * - The first-in element is [KEY_1] with [VAL_1]
     * - The last-in element is [KEY_4] with [VAL_4]
     */
    private suspend fun InMemoryKache<String, Int>.prepareKache() {
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        put(KEY_3, VAL_3)
        put(KEY_4, VAL_4)
        get(KEY_1)
        get(KEY_2)
    }
}
