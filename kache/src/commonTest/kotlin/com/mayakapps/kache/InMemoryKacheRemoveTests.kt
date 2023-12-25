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

import kotlinx.coroutines.CancellationException
import kotlin.test.*

class InMemoryKacheRemoveTests {

    /*
     * remove tests
     */

    @Test
    fun testRemoveNonExisting() = runBasicInMemoryKacheTest {
        assertNull(remove(KEY_1))
    }

    @Test
    fun testRemoveExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        assertEquals(VAL_1, remove(KEY_1))
        assertNull(getIfAvailable(KEY_1))
    }

    @Test
    fun testRemoveCreating() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_1 }
        assertNull(remove(KEY_1))
        assertFailsWith<CancellationException> { deferred.await() }
        assertNull(get(KEY_1))
    }

    @Test
    fun testRemoveExistingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        remove(KEY_1)

        assertEquals(1, removedEntries.size)
        removedEntries.firstOrNull()?.run {
            assertFalse(evicted)
            assertEquals(KEY_1, key)
            assertEquals(VAL_1, oldValue)
            assertNull(newValue)
        }
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun testRemoveCreatingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        putAsync(KEY_1) { VAL_1 }
        remove(KEY_1)

        assertEquals(0, removedEntries.size)
    }

    /*
     * clear tests
     */

    @Test
    fun testClearExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        clear()
        assertNull(getIfAvailable(KEY_1))
    }

    @Test
    fun testClearCreating() = runBasicInMemoryKacheTest {
        val deferred1 = putAsync(KEY_1) { VAL_1 }
        val deferred2 = putAsync(KEY_2) { VAL_2 }
        clear()
        assertFailsWith<CancellationException> { deferred1.await() }
        assertFailsWith<CancellationException> { deferred2.await() }
        assertNull(get(KEY_1))
        assertNull(get(KEY_2))
    }

    @Test
    fun testClearExistingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        clear()

        assertEquals(2, removedEntries.size)
        removedEntries.getOrNull(0)?.run {
            assertFalse(evicted)
            assertEquals(KEY_1, key)
            assertEquals(VAL_1, oldValue)
            assertNull(newValue)
        }

        removedEntries.getOrNull(1)?.run {
            assertFalse(evicted)
            assertEquals(KEY_2, key)
            assertEquals(VAL_2, oldValue)
            assertNull(newValue)
        }
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun testClearCreatingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        putAsync(KEY_1) { VAL_1 }
        putAsync(KEY_2) { VAL_2 }
        clear()

        assertEquals(0, removedEntries.size)
    }

    /*
     * removeAllUnderCreation() tests
     */

    @Test
    fun testRemoveAllUnderCreation() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        val deferred = putAsync(KEY_2) { VAL_2 }
        removeAllUnderCreation()

        assertFailsWith<CancellationException> { deferred.await() }
        assertEquals(VAL_1, get(KEY_1))
        assertNull(get(KEY_2))
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun testRemoveAllUnderCreationTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        putAsync(KEY_1) { VAL_1 }
        putAsync(KEY_2) { VAL_2 }
        removeAllUnderCreation()

        assertEquals(0, removedEntries.size)
    }
}
