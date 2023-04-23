package com.mayakapps.kache

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryKachePutTests {

    /*
     * getOrPut tests
     */

    @Test
    fun testGetOrPutNewCreatedSuccessfully() = runBasicInMemoryKacheTest {
        getOrPut(KEY_1) { VAL_1 } shouldBe VAL_1.asPutResult()
    }

    @Test
    fun testGetOrPutNewFailedCreating() = runBasicInMemoryKacheTest {
        getOrPut(KEY_1) { null } shouldBe null.asPutResult()
    }

    @Test
    fun testGetOrPutExistingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        getOrPut(KEY_1) { VAL_2 } shouldBe VAL_1.asPutResult()
    }

    @Test
    fun testGetOrPutExistingFailedCreating() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        getOrPut(KEY_1) { null } shouldBe VAL_1.asPutResult()
    }

    @Test
    fun testGetOrPutCreatingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_1 }
        getOrPut(KEY_1) { VAL_2 } shouldBe VAL_1.asPutResult()
        shouldNotThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testGetOrPutCreatingFailedCreating() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_1 }
        getOrPut(KEY_1) { null } shouldBe VAL_1.asPutResult()
        shouldNotThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
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

        testScope.currentTime shouldBe 1L
    }

    /*
     * put tests
     */

    @Test
    fun testPutNew() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1) shouldBe null.asOldValue()
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testPutExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_1, VAL_2) shouldBe VAL_1.asOldValue()
        getIfAvailable(KEY_1) shouldBe VAL_2.asValue()
    }

    @Test
    fun testPutExistingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_1, VAL_2)
        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe false.asEvicted()
            key shouldBe KEY_1.asKey()
            oldValue shouldBe VAL_1.asOldValue()
            newValue shouldBe VAL_2.asValue()
        }
    }

    @Test
    fun testPutCreating() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_2 }
        put(KEY_1, VAL_1) shouldBe null.asOldValue()
        shouldThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
    }

    /*
     * put with creation function tests
     */

    @Test
    fun testPutNewCreatedSuccessfully() = runBasicInMemoryKacheTest {
        put(KEY_1) { VAL_1 } shouldBe VAL_1.asPutResult()
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testPutNewFailedCreating() = runBasicInMemoryKacheTest {
        put(KEY_1) { null } shouldBe null.asPutResult()
        getIfAvailable(KEY_1) shouldBe null.asValue()
    }

    @Test
    fun testPutExistingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_2)
        put(KEY_1) { VAL_1 } shouldBe VAL_1.asPutResult()
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testPutExistingCreatedSuccessfullyTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_2)
        put(KEY_1) { VAL_1 }

        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe false.asEvicted()
            key shouldBe KEY_1.asKey()
            oldValue shouldBe VAL_2.asOldValue()
            newValue shouldBe VAL_1.asValue()
        }
    }

    @Test
    fun testPutExistingFailedCreating() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_1) { null } shouldBe null.asPutResult()
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testPutExistingFailedCreatingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_1) { null }

        removedEntries shouldHaveSize 0
    }

    @Test
    fun testPutCreatingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_2 }
        put(KEY_1) { VAL_1 } shouldBe VAL_1.asPutResult()
        shouldThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testPutCreatingCreationFailed() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_1 }
        put(KEY_1) { null } shouldBe null.asPutResult()
        shouldThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY_1) shouldBe null.asValue()
    }

    /*
     * putAsync tests
     */

    @Test
    fun testPutAsyncNewCreatedSuccessfully() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_1 }
        getIfAvailable(KEY_1) shouldBe null.asOldValue()
        deferred.await() shouldBe VAL_1.asPutResult()
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testPutAsyncNewFailedCreating() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { null }
        getIfAvailable(KEY_1) shouldBe null.asOldValue()
        deferred.await() shouldBe null.asPutResult()
        getIfAvailable(KEY_1) shouldBe null.asValue()
    }

    @Test
    fun testPutAsyncExistingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_2)
        val deferred = putAsync(KEY_1) { VAL_1 }
        getIfAvailable(KEY_1) shouldBe VAL_2.asOldValue()
        deferred.await() shouldBe VAL_1.asPutResult()
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testPutAsyncExistingCreatedSuccessfullyTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_2)
        putAsync(KEY_1) { VAL_1 }.await()

        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe false.asEvicted()
            key shouldBe KEY_1.asKey()
            oldValue shouldBe VAL_2.asOldValue()
            newValue shouldBe VAL_1.asValue()
        }
    }

    @Test
    fun testPutAsyncExistingFailedCreating() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        val deferred = putAsync(KEY_1) { null }
        getIfAvailable(KEY_1) shouldBe VAL_1.asOldValue()
        deferred.await() shouldBe null.asPutResult()
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testPutAsyncExistingFailedCreatingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        putAsync(KEY_1) { null }.await()

        removedEntries shouldHaveSize 0
    }

    @Test
    fun testPutAsyncCreatingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        val oldDeferred = putAsync(KEY_1) { VAL_2 }
        val deferred = putAsync(KEY_1) { VAL_1 }
        shouldThrow<CancellationException> { oldDeferred.await() }
        deferred.await() shouldBe VAL_1.asPutResult()
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testPutAsyncCreatingCreationFailed() = runBasicInMemoryKacheTest {
        val oldDeferred = putAsync(KEY_1) { VAL_1 }
        val deferred = putAsync(KEY_1) { null }
        shouldThrow<CancellationException> { oldDeferred.await() }
        deferred.await() shouldBe null.asPutResult()
        getIfAvailable(KEY_1) shouldBe null.asValue()
    }

    /*
     * Nesting Tests
     */

    @Test
    fun testGetIfAvailableSameKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_1) {
            getIfAvailable(KEY_1) shouldBe VAL_1
            VAL_2
        }
    }

    @Test
    fun testGetIfAvailableDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_2) {
            getIfAvailable(KEY_1) shouldBe VAL_1
            VAL_2
        }
    }

    @Test
    fun testGetDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_2) {
            get(KEY_1) shouldBe VAL_1
            VAL_2
        }
    }

    @Test
    fun testGetOrPutDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_2) {
            getOrPut(KEY_1) { VAL_1 } shouldBe VAL_1
            VAL_2
        }
    }

    @Test
    fun testPutSameKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY_1) {
            put(KEY_1, VAL_2)
            VAL_1
        }

        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
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
        size shouldBe 0.asSize()

    }
}