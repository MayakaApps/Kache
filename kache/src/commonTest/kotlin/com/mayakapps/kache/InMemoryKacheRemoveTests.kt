package com.mayakapps.kache

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryKacheRemoveTests {

    /*
     * remove tests
     */

    @Test
    fun testRemoveNonExisting() = runBasicInMemoryKacheTest {
        remove(KEY) shouldBe null.asOldValue()
    }

    @Test
    fun testRemoveExisting() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        remove(KEY) shouldBe VAL.asOldValue()
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    @Test
    fun testRemoveCreating() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY) { VAL }
        remove(KEY) shouldBe null.asOldValue()
        shouldThrow<CancellationException> { deferred.await() }
        get(KEY) shouldBe null.asValue()
    }

    @Test
    fun testRemoveExistingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
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
    fun testRemoveCreatingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        putAsync(KEY) { VAL }
        remove(KEY)

        removedEntries shouldHaveSize 0
    }

    /*
     * clear tests
     */

    @Test
    fun testClearExisting() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        clear()
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    @Test
    fun testClearCreating() = runBasicInMemoryKacheTest {
        val deferred1 = putAsync(KEY) { VAL }
        val deferred2 = putAsync(ALT_KEY) { ALT_VAL }
        clear()
        shouldThrow<CancellationException> { deferred1.await() }
        shouldThrow<CancellationException> { deferred2.await() }
        get(KEY) shouldBe null.asValue()
        get(ALT_KEY) shouldBe null.asValue()
    }

    @Test
    fun testClearExistingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
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
    fun testClearCreatingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        putAsync(KEY) { VAL }
        putAsync(ALT_KEY) { ALT_VAL }
        clear()

        removedEntries shouldHaveSize 0
    }

    /*
     * removeAllUnderCreation() tests
     */

    @Test
    fun testRemoveAllUnderCreation() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        val deferred = putAsync(ALT_KEY) { ALT_VAL }
        removeAllUnderCreation()
        shouldThrow<CancellationException> { deferred.await() }
        get(KEY) shouldBe VAL.asValue()
        get(ALT_KEY) shouldBe null.asValue()
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun testRemoveAllUnderCreationTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        putAsync(KEY) { VAL }
        putAsync(ALT_KEY) { ALT_VAL }
        removeAllUnderCreation()

        removedEntries shouldHaveSize 0
    }
}