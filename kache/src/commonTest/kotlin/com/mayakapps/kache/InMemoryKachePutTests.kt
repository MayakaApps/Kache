package com.mayakapps.kache

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryKachePutTests {

    /*
     * getOrPut tests
     */

    @Test
    fun testGetOrPutNewCreatedSuccessfully() = runBasicInMemoryKacheTest {
        getOrPut(KEY) { VAL } shouldBe VAL.asPutResult()
    }

    @Test
    fun testGetOrPutNewFailedCreating() = runBasicInMemoryKacheTest {
        getOrPut(KEY) { null } shouldBe null.asPutResult()
    }

    @Test
    fun testGetOrPutExistingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        getOrPut(KEY) { ALT_VAL } shouldBe VAL.asPutResult()
    }

    @Test
    fun testGetOrPutExistingFailedCreating() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        getOrPut(KEY) { null } shouldBe VAL.asPutResult()
    }

    @Test
    fun testGetOrPutCreatingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY) { VAL }
        getOrPut(KEY) { ALT_VAL } shouldBe VAL.asPutResult()
        shouldNotThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testGetOrPutCreatingFailedCreating() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY) { VAL }
        getOrPut(KEY) { null } shouldBe VAL.asPutResult()
        shouldNotThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    /*
     * put tests
     */

    @Test
    fun testPutNew() = runBasicInMemoryKacheTest {
        put(KEY, VAL) shouldBe null.asOldValue()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutExisting() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        put(KEY, ALT_VAL) shouldBe VAL.asOldValue()
        getIfAvailable(KEY) shouldBe ALT_VAL.asValue()
    }

    @Test
    fun testPutExistingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY, VAL)
        put(KEY, ALT_VAL)
        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe false.asEvicted()
            key shouldBe KEY.asKey()
            oldValue shouldBe VAL.asOldValue()
            newValue shouldBe ALT_VAL.asValue()
        }
    }

    @Test
    fun testPutCreating() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY) { ALT_VAL }
        put(KEY, VAL) shouldBe null.asOldValue()
        shouldThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    /*
     * put with creation function tests
     */

    @Test
    fun testPutNewCreatedSuccessfully() = runBasicInMemoryKacheTest {
        put(KEY) { VAL } shouldBe VAL.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutNewFailedCreating() = runBasicInMemoryKacheTest {
        put(KEY) { null } shouldBe null.asPutResult()
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    @Test
    fun testPutExistingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        put(KEY, ALT_VAL)
        put(KEY) { VAL } shouldBe VAL.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutExistingCreatedSuccessfullyTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY, ALT_VAL)
        put(KEY) { VAL }

        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe false.asEvicted()
            key shouldBe KEY.asKey()
            oldValue shouldBe ALT_VAL.asOldValue()
            newValue shouldBe VAL.asValue()
        }
    }

    @Test
    fun testPutExistingFailedCreating() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        put(KEY) { null } shouldBe null.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutExistingFailedCreatingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY, VAL)
        put(KEY) { null }

        removedEntries shouldHaveSize 0
    }

    @Test
    fun testPutCreatingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY) { ALT_VAL }
        put(KEY) { VAL } shouldBe VAL.asPutResult()
        shouldThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutCreatingCreationFailed() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY) { VAL }
        put(KEY) { null } shouldBe null.asPutResult()
        shouldThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    /*
     * putAsync tests
     */

    @Test
    fun testPutAsyncNewCreatedSuccessfully() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY) { VAL }
        getIfAvailable(KEY) shouldBe null.asOldValue()
        deferred.await() shouldBe VAL.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutAsyncNewFailedCreating() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY) { null }
        getIfAvailable(KEY) shouldBe null.asOldValue()
        deferred.await() shouldBe null.asPutResult()
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    @Test
    fun testPutAsyncExistingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        put(KEY, ALT_VAL)
        val deferred = putAsync(KEY) { VAL }
        getIfAvailable(KEY) shouldBe ALT_VAL.asOldValue()
        deferred.await() shouldBe VAL.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutAsyncExistingCreatedSuccessfullyTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY, ALT_VAL)
        putAsync(KEY) { VAL }.await()

        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe false.asEvicted()
            key shouldBe KEY.asKey()
            oldValue shouldBe ALT_VAL.asOldValue()
            newValue shouldBe VAL.asValue()
        }
    }

    @Test
    fun testPutAsyncExistingFailedCreating() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        val deferred = putAsync(KEY) { null }
        getIfAvailable(KEY) shouldBe VAL.asOldValue()
        deferred.await() shouldBe null.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutAsyncExistingFailedCreatingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY, VAL)
        putAsync(KEY) { null }.await()

        removedEntries shouldHaveSize 0
    }

    @Test
    fun testPutAsyncCreatingCreatedSuccessfully() = runBasicInMemoryKacheTest {
        val oldDeferred = putAsync(KEY) { ALT_VAL }
        val deferred = putAsync(KEY) { VAL }
        shouldThrow<CancellationException> { oldDeferred.await() }
        deferred.await() shouldBe VAL.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutAsyncCreatingCreationFailed() = runBasicInMemoryKacheTest {
        val oldDeferred = putAsync(KEY) { VAL }
        val deferred = putAsync(KEY) { null }
        shouldThrow<CancellationException> { oldDeferred.await() }
        deferred.await() shouldBe null.asPutResult()
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    /*
     * Nesting Tests
     */

    @Test
    fun testGetIfAvailableSameKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        put(KEY) {
            getIfAvailable(KEY) shouldBe VAL
            ALT_VAL
        }
    }

    @Test
    fun testGetIfAvailableDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        put(ALT_KEY) {
            getIfAvailable(KEY) shouldBe VAL
            ALT_VAL
        }
    }

    @Test
    fun testGetDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        put(ALT_KEY) {
            get(KEY) shouldBe VAL
            ALT_VAL
        }
    }

    @Test
    fun testGetOrPutDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(ALT_KEY) {
            getOrPut(KEY) { VAL } shouldBe VAL
            ALT_VAL
        }
    }

    @Test
    fun testPutSameKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY) {
            put(KEY, ALT_VAL)
            VAL
        }

        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY) {
            put(ALT_KEY, VAL)
            ALT_VAL
        }
    }

    @Test
    fun testRemoveSameKeyInsidePut() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        put(KEY) {
            remove(KEY)
            ALT_VAL
        }
    }

    @Test
    fun testRemoveDifferentKeyInsidePut() = runBasicInMemoryKacheTest {
        put(ALT_KEY, ALT_VAL)
        put(KEY) {
            remove(ALT_KEY)
            VAL
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
        put(KEY, VAL)
        size shouldBe 0.asSize()

    }
}