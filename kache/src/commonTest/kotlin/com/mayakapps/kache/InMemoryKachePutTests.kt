package com.mayakapps.kache

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlin.test.*

class InMemoryKachePutTests {

    /*
     * getOrPut tests
     */

    @Test
    fun testGetOrPutNewCreatedSuccessfully() = runBasicInMemoryKacheTest {
        assertEquals(VAL_1, getOrPut(KEY_1) { VAL_1 })
    }

    @Test
    fun testGetOrPutNewFailedCreating() = runBasicInMemoryKacheTest {
        assertNull(getOrPut(KEY_1) { null })
    }

    @Test
    fun testGetOrPutExistingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        assertEquals(VAL_1, getOrPut(KEY_1) { VAL_2 })
    }

    @Test
    fun testGetOrPutExistingFailedCreating() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        assertEquals(VAL_1, getOrPut(KEY_1) { null })
    }

    @Test
    fun testGetOrPutCreatingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_1 }
        assertEquals(VAL_1, getOrPut(KEY_1) { VAL_2 })
        try {
            deferred.await()
        } catch (e: CancellationException) {
            fail("Deferred should not be cancelled")
        }
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    @Test
    fun testGetOrPutCreatingFailedCreating() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_1 }
        assertEquals(VAL_1, getOrPut(KEY_1) { null })
        try {
            deferred.await()
        } catch (e: CancellationException) {
            fail("Deferred should not be cancelled")
        }
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    // See issue #50 (https://github.com/MayakaApps/Kache/issues/50) for more details
    @Test
    fun testGetOrPutSimultaneous() = runBasicInMemoryKacheTest { testScope ->
        @Suppress("DeferredResultUnused")
        putAsync(KEY_1) {
            delay(60_000)
            VAL_1
        }

        testScope.launch { getOrPut(KEY_1) { VAL_1 } }
        delay(1)
        put(KEY_2) { VAL_2 }

        @OptIn(ExperimentalCoroutinesApi::class)
        assertEquals(1L, testScope.currentTime)
    }

    /*
     * put tests
     */

    @Test
    fun testPutNew() = runBasicInMemoryKacheTest {
        assertNull(put(KEY_1, VAL_1))
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    @Test
    fun testPutExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        assertEquals(VAL_1, put(KEY_1, VAL_2))
        assertEquals(VAL_2, getIfAvailable(KEY_1))
    }

    @Test
    fun testPutExistingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_1, VAL_2)
        assertEquals(1, removedEntries.size)
        removedEntries.firstOrNull()?.run {
            assertEquals(false, evicted)
            assertEquals(KEY_1, key)
            assertEquals(VAL_1, oldValue)
            assertEquals(VAL_2, newValue)
        }
    }

    @Test
    fun testPutCreating() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_2 }
        assertNull(put(KEY_1, VAL_1))
        assertFailsWith<CancellationException> { deferred.await() }
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    /*
     * put with creation function tests
     */

    @Test
    fun testPutNewCreatedSuccessfully() = runBasicInMemoryKacheTest {
        assertEquals(VAL_1, put(KEY_1) { VAL_1 })
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    @Test
    fun testPutNewFailedCreating() = runBasicInMemoryKacheTest {
        assertNull(put(KEY_1) { null })
        assertNull(getIfAvailable(KEY_1))
    }

    @Test
    fun testPutExistingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_2)
        assertEquals(VAL_1, put(KEY_1) { VAL_1 })
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    @Test
    fun testPutExistingCreatedSuccessfullyTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_2)
        put(KEY_1) { VAL_1 }

        assertEquals(1, removedEntries.size)
        removedEntries.firstOrNull()?.run {
            assertEquals(false, evicted)
            assertEquals(KEY_1, key)
            assertEquals(VAL_2, oldValue)
            assertEquals(VAL_1, newValue)
        }
    }

    @Test
    fun testPutExistingFailedCreating() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        assertNull(put(KEY_1) { null })
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    @Test
    fun testPutExistingFailedCreatingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_1) { null }

        assertEquals(0, removedEntries.size)
    }

    @Test
    fun testPutCreatingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_2 }
        assertEquals(VAL_1, put(KEY_1) { VAL_1 })
        assertFailsWith<CancellationException> { deferred.await() }
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    @Test
    fun testPutCreatingCreationFailed() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_1 }
        assertNull(put(KEY_1) { null })
        assertFailsWith<CancellationException> { deferred.await() }
        assertNull(getIfAvailable(KEY_1))
    }

    /*
     * putAsync tests
     */

    @Test
    fun testPutAsyncNewCreatedSuccessfully() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_1 }
        assertNull(getIfAvailable(KEY_1))
        assertEquals(VAL_1, deferred.await())
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    @Test
    fun testPutAsyncNewFailedCreating() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { null }
        assertNull(getIfAvailable(KEY_1))
        assertNull(deferred.await())
        assertNull(getIfAvailable(KEY_1))
    }

    @Test
    fun testPutAsyncExistingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_2)
        val deferred = putAsync(KEY_1) { VAL_1 }
        assertEquals(VAL_2, getIfAvailable(KEY_1))
        assertEquals(VAL_1, deferred.await())
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    @Test
    fun testPutAsyncExistingCreatedSuccessfullyTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_2)
        putAsync(KEY_1) { VAL_1 }.await()

        assertEquals(1, removedEntries.size)
        removedEntries.firstOrNull()?.run {
            assertEquals(false, evicted)
            assertEquals(KEY_1, key)
            assertEquals(VAL_2, oldValue)
            assertEquals(VAL_1, newValue)
        }
    }

    @Test
    fun testPutAsyncExistingFailedCreating() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        val deferred = putAsync(KEY_1) { null }
        assertEquals(VAL_1, getIfAvailable(KEY_1))
        assertNull(deferred.await())
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    @Test
    fun testPutAsyncExistingFailedCreatingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        putAsync(KEY_1) { null }.await()

        assertEquals(0, removedEntries.size)
    }

    @Test
    fun testPutAsyncCreatingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        val oldDeferred = putAsync(KEY_1) { VAL_2 }
        val deferred = putAsync(KEY_1) { VAL_1 }
        assertFailsWith<CancellationException> { oldDeferred.await() }
        assertEquals(VAL_1, deferred.await())
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    @Test
    fun testPutAsyncCreatingCreationFailed() = runBasicInMemoryKacheTest {
        val oldDeferred = putAsync(KEY_1) { VAL_1 }
        val deferred = putAsync(KEY_1) { null }
        assertFailsWith<CancellationException> { oldDeferred.await() }
        assertNull(deferred.await())
        assertNull(getIfAvailable(KEY_1))
    }

    /*
     * Nesting Tests
     */

    @Test
    fun testGetIfAvailableSameKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_1) {
            assertEquals(VAL_1, getIfAvailable(KEY_1))
            VAL_2
        }
    }

    @Test
    fun testGetIfAvailableDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_2) {
            assertEquals(VAL_1, getIfAvailable(KEY_1))
            VAL_2
        }
    }

    @Test
    fun testGetDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_2) {
            assertEquals(VAL_1, get(KEY_1))
            VAL_2
        }
    }

    @Test
    fun testGetOrPutDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_2) {
            assertEquals(VAL_1, getOrPut(KEY_1) { VAL_1 })
            VAL_2
        }
    }

    @Test
    fun testPutSameKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_1) {
            put(KEY_1, VAL_2)
            VAL_1
        }

        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    @Test
    fun testPutDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_1) {
            put(KEY_2, VAL_1)
            VAL_2
        }
    }

    @Test
    fun testRemoveSameKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_1) {
            remove(KEY_1)
            VAL_2
        }
    }

    @Test
    fun testRemoveDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_2, VAL_2)
        put(KEY_1) {
            remove(KEY_2)
            VAL_1
        }
    }

    /*
     * More Test Cases
     */

    @Test
    fun testPutNewBiggerThanMaxSize() = runBasicInMemoryKacheRemoveListenerTest(
        maxSize = 10,
        sizeCalculator = { _, _ -> 20 },
    ) {
        put(KEY_1, VAL_1)
        assertEquals(0, size)
    }
}
