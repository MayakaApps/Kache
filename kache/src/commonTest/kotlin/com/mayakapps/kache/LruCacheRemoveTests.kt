package com.mayakapps.kache

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LruCacheRemoveTests {

    /*
     * remove tests
     */

    @Test
    fun testRemoveNonExisting() = runBasicLruCacheTest {
        remove(KEY) shouldBe null.asOldValue()
    }

    @Test
    fun testRemoveExisting() = runBasicLruCacheTest {
        put(KEY, VAL)
        remove(KEY) shouldBe VAL.asOldValue()
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    @Test
    fun testRemoveCreating() = runBasicLruCacheTest {
        val deferred = putAsync(KEY) { VAL }
        remove(KEY) shouldBe null.asOldValue()
        shouldThrow<CancellationException> { deferred.await() }
        get(KEY) shouldBe null.asValue()
    }

    @Test
    fun testRemoveExistingTrigger() = runBasicLruCacheRemoveListenerTest { removedEntries ->
        put(KEY, VAL)
        remove(KEY)

        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe false.asEvicted()
            key shouldBe KEY.asKey()
            oldValue shouldBe VAL.asOldValue()
            newValue shouldBe null.asValue()
        }
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun testRemoveCreatingTrigger() = runBasicLruCacheRemoveListenerTest { removedEntries ->
        putAsync(KEY) { VAL }
        remove(KEY)

        removedEntries shouldHaveSize 0
    }

    /*
     * clear tests
     */

    @Test
    fun testClearExisting() = runBasicLruCacheTest {
        put(KEY, VAL)
        clear()
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    @Test
    fun testClearCreating() = runBasicLruCacheTest {
        val deferred1 = putAsync(KEY) { VAL }
        val deferred2 = putAsync(ALT_KEY) { ALT_VAL }
        clear()
        shouldThrow<CancellationException> { deferred1.await() }
        shouldThrow<CancellationException> { deferred2.await() }
        get(KEY) shouldBe null.asValue()
        get(ALT_KEY) shouldBe null.asValue()
    }

    @Test
    fun testClearExistingTrigger() = runBasicLruCacheRemoveListenerTest { removedEntries ->
        put(KEY, VAL)
        put(ALT_KEY, ALT_VAL)
        clear()

        removedEntries shouldHaveSize 2
        removedEntries.getOrNull(0)?.run {
            evicted shouldBe false.asEvicted()
            key shouldBe KEY.asKey()
            oldValue shouldBe VAL.asOldValue()
            newValue shouldBe null.asValue()
        }

        removedEntries.getOrNull(1)?.run {
            evicted shouldBe false.asEvicted()
            key shouldBe ALT_KEY.asKey()
            oldValue shouldBe ALT_VAL.asOldValue()
            newValue shouldBe null.asValue()
        }
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun testClearCreatingTrigger() = runBasicLruCacheRemoveListenerTest { removedEntries ->
        putAsync(KEY) { VAL }
        putAsync(ALT_KEY) { ALT_VAL }
        clear()

        removedEntries shouldHaveSize 0
    }

    /*
     * removeAllUnderCreation() tests
     */

    @Test
    fun testRemoveAllUnderCreation() = runBasicLruCacheTest {
        put(KEY, VAL)
        val deferred = putAsync(ALT_KEY) { ALT_VAL }
        removeAllUnderCreation()
        shouldThrow<CancellationException> { deferred.await() }
        get(KEY) shouldBe VAL.asValue()
        get(ALT_KEY) shouldBe null.asValue()
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun testRemoveAllUnderCreationTrigger() = runBasicLruCacheRemoveListenerTest { removedEntries ->
        putAsync(KEY) { VAL }
        putAsync(ALT_KEY) { ALT_VAL }
        removeAllUnderCreation()

        removedEntries shouldHaveSize 0
    }
}