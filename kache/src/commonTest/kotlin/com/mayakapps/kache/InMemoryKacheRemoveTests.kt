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
        remove(KEY_1) shouldBe null.asOldValue()
    }

    @Test
    fun testRemoveExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        remove(KEY_1) shouldBe VAL_1.asOldValue()
        getIfAvailable(KEY_1) shouldBe null.asValue()
    }

    @Test
    fun testRemoveCreating() = runBasicInMemoryKacheTest {
        val deferred = putAsync(KEY_1) { VAL_1 }
        remove(KEY_1) shouldBe null.asOldValue()
        shouldThrow<CancellationException> { deferred.await() }
        get(KEY_1) shouldBe null.asValue()
    }

    @Test
    fun testRemoveExistingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        remove(KEY_1)

        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe false.asEvicted()
            key shouldBe KEY_1.asKey()
            oldValue shouldBe VAL_1.asOldValue()
            newValue shouldBe null.asValue()
        }
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun testRemoveCreatingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        putAsync(KEY_1) { VAL_1 }
        remove(KEY_1)

        removedEntries shouldHaveSize 0
    }

    /*
     * clear tests
     */

    @Test
    fun testClearExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        clear()
        getIfAvailable(KEY_1) shouldBe null.asValue()
    }

    @Test
    fun testClearCreating() = runBasicInMemoryKacheTest {
        val deferred1 = putAsync(KEY_1) { VAL_1 }
        val deferred2 = putAsync(KEY_2) { VAL_2 }
        clear()
        shouldThrow<CancellationException> { deferred1.await() }
        shouldThrow<CancellationException> { deferred2.await() }
        get(KEY_1) shouldBe null.asValue()
        get(KEY_2) shouldBe null.asValue()
    }

    @Test
    fun testClearExistingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        clear()

        removedEntries shouldHaveSize 2
        removedEntries.getOrNull(0)?.run {
            evicted shouldBe false.asEvicted()
            key shouldBe KEY_1.asKey()
            oldValue shouldBe VAL_1.asOldValue()
            newValue shouldBe null.asValue()
        }

        removedEntries.getOrNull(1)?.run {
            evicted shouldBe false.asEvicted()
            key shouldBe KEY_2.asKey()
            oldValue shouldBe VAL_2.asOldValue()
            newValue shouldBe null.asValue()
        }
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun testClearCreatingTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        putAsync(KEY_1) { VAL_1 }
        putAsync(KEY_2) { VAL_2 }
        clear()

        removedEntries shouldHaveSize 0
    }

    /*
     * removeAllUnderCreation() tests
     */

    @Test
    fun testRemoveAllUnderCreation() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        val deferred = putAsync(KEY_2) { VAL_2 }
        removeAllUnderCreation()
        shouldThrow<CancellationException> { deferred.await() }
        get(KEY_1) shouldBe VAL_1.asValue()
        get(KEY_2) shouldBe null.asValue()
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun testRemoveAllUnderCreationTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        putAsync(KEY_1) { VAL_1 }
        putAsync(KEY_2) { VAL_2 }
        removeAllUnderCreation()

        removedEntries shouldHaveSize 0
    }
}