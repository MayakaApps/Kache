package com.mayakapps.lrucache

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LruCachePutTests {

    /*
     * getOrPut tests
     */

    @Test
    fun testGetOrPutNewCreatedSuccessfully() = runBasicLruCacheTest {
        getOrPut(KEY) { VAL } shouldBe VAL.asPutResult()
    }

    @Test
    fun testGetOrPutNewFailedCreating() = runBasicLruCacheTest {
        getOrPut(KEY) { null } shouldBe null.asPutResult()
    }

    @Test
    fun testGetOrPutExistingCreatedSuccessfully() = runBasicLruCacheTest {
        put(KEY, VAL)
        getOrPut(KEY) { ALT_VAL } shouldBe VAL.asPutResult()
    }

    @Test
    fun testGetOrPutExistingFailedCreating() = runBasicLruCacheTest {
        put(KEY, VAL)
        getOrPut(KEY) { null } shouldBe VAL.asPutResult()
    }

    @Test
    fun testGetOrPutCreatingCreatedSuccessfully() = runBasicLruCacheTest {
        val deferred = putAsync(KEY) { VAL }
        getOrPut(KEY) { ALT_VAL } shouldBe VAL.asPutResult()
        shouldNotThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testGetOrPutCreatingFailedCreating() = runBasicLruCacheTest {
        val deferred = putAsync(KEY) { VAL }
        getOrPut(KEY) { null } shouldBe VAL.asPutResult()
        shouldNotThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    /*
     * put tests
     */

    @Test
    fun testPutNew() = runBasicLruCacheTest {
        put(KEY, VAL) shouldBe null.asOldValue()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutExisting() = runBasicLruCacheTest {
        put(KEY, VAL)
        put(KEY, ALT_VAL) shouldBe VAL.asOldValue()
        getIfAvailable(KEY) shouldBe ALT_VAL.asValue()
    }

    @Test
    fun testPutExistingTrigger() = runBasicLruCacheRemoveListenerTest { removedEntries ->
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
    fun testPutCreating() = runBasicLruCacheTest {
        val deferred = putAsync(KEY) { ALT_VAL }
        put(KEY, VAL) shouldBe null.asOldValue()
        shouldThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    /*
     * put with creation function tests
     */

    @Test
    fun testPutNewCreatedSuccessfully() = runBasicLruCacheTest {
        put(KEY) { VAL } shouldBe VAL.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutNewFailedCreating() = runBasicLruCacheTest {
        put(KEY) { null } shouldBe null.asPutResult()
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    @Test
    fun testPutExistingCreatedSuccessfully() = runBasicLruCacheTest {
        put(KEY, ALT_VAL)
        put(KEY) { VAL } shouldBe VAL.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutExistingCreatedSuccessfullyTrigger() = runBasicLruCacheRemoveListenerTest { removedEntries ->
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
    fun testPutExistingFailedCreating() = runBasicLruCacheTest {
        put(KEY, VAL)
        put(KEY) { null } shouldBe null.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutExistingFailedCreatingTrigger() = runBasicLruCacheRemoveListenerTest { removedEntries ->
        put(KEY, VAL)
        put(KEY) { null }

        removedEntries shouldHaveSize 0
    }

    @Test
    fun testPutCreatingCreatedSuccessfully() = runBasicLruCacheTest {
        val deferred = putAsync(KEY) { ALT_VAL }
        put(KEY) { VAL } shouldBe VAL.asPutResult()
        shouldThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutCreatingCreationFailed() = runBasicLruCacheTest {
        val deferred = putAsync(KEY) { VAL }
        put(KEY) { null } shouldBe null.asPutResult()
        shouldThrow<CancellationException> { deferred.await() }
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    /*
     * putAsync tests
     */

    @Test
    fun testPutAsyncNewCreatedSuccessfully() = runBasicLruCacheTest {
        val deferred = putAsync(KEY) { VAL }
        getIfAvailable(KEY) shouldBe null.asOldValue()
        deferred.await() shouldBe VAL.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutAsyncNewFailedCreating() = runBasicLruCacheTest {
        val deferred = putAsync(KEY) { null }
        getIfAvailable(KEY) shouldBe null.asOldValue()
        deferred.await() shouldBe null.asPutResult()
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    @Test
    fun testPutAsyncExistingCreatedSuccessfully() = runBasicLruCacheTest {
        put(KEY, ALT_VAL)
        val deferred = putAsync(KEY) { VAL }
        getIfAvailable(KEY) shouldBe ALT_VAL.asOldValue()
        deferred.await() shouldBe VAL.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutAsyncExistingCreatedSuccessfullyTrigger() = runBasicLruCacheRemoveListenerTest { removedEntries ->
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
    fun testPutAsyncExistingFailedCreating() = runBasicLruCacheTest {
        put(KEY, VAL)
        val deferred = putAsync(KEY) { null }
        getIfAvailable(KEY) shouldBe VAL.asOldValue()
        deferred.await() shouldBe null.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutAsyncExistingFailedCreatingTrigger() = runBasicLruCacheRemoveListenerTest { removedEntries ->
        put(KEY, VAL)
        putAsync(KEY) { null }.await()

        removedEntries shouldHaveSize 0
    }

    @Test
    fun testPutAsyncCreatingCreatedSuccessfully() = runBasicLruCacheTest {
        val oldDeferred = putAsync(KEY) { ALT_VAL }
        val deferred = putAsync(KEY) { VAL }
        shouldThrow<CancellationException> { oldDeferred.await() }
        deferred.await() shouldBe VAL.asPutResult()
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutAsyncCreatingCreationFailed() = runBasicLruCacheTest {
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
    fun testGetIfAvailableSameKeyInsidePut() = runBasicLruCacheTest {
        put(KEY, VAL)
        put(KEY) {
            getIfAvailable(KEY) shouldBe VAL
            ALT_VAL
        }
    }

    @Test
    fun testGetIfAvailableDifferentKeyInsidePut() = runBasicLruCacheTest {
        put(KEY, VAL)
        put(ALT_KEY) {
            getIfAvailable(KEY) shouldBe VAL
            ALT_VAL
        }
    }

    @Test
    fun testGetDifferentKeyInsidePut() = runBasicLruCacheTest {
        put(KEY, VAL)
        put(ALT_KEY) {
            get(KEY) shouldBe VAL
            ALT_VAL
        }
    }

    @Test
    fun testGetOrPutDifferentKeyInsidePut() = runBasicLruCacheTest {
        put(ALT_KEY) {
            getOrPut(KEY) { VAL } shouldBe VAL
            ALT_VAL
        }
    }

    @Test
    fun testPutSameKeyInsidePut() = runBasicLruCacheTest {
        put(KEY) {
            put(KEY, ALT_VAL)
            VAL
        }

        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testPutDifferentKeyInsidePut() = runBasicLruCacheTest {
        put(KEY) {
            put(ALT_KEY, VAL)
            ALT_VAL
        }
    }

    @Test
    fun testRemoveSameKeyInsidePut() = runBasicLruCacheTest {
        put(KEY, VAL)
        put(KEY) {
            remove(KEY)
            ALT_VAL
        }
    }

    @Test
    fun testRemoveDifferentKeyInsidePut() = runBasicLruCacheTest {
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
    fun testPutNewBiggerThanMaxSize() = runBasicLruCacheRemoveListenerTest(
        maxSize = 10,
        sizeCalculator = { _, _ -> 20 },
    ) {
        put(KEY, VAL)
        size shouldBe 0.asSize()

    }
}