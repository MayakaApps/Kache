package com.mayakapps.kache

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryKacheSizeTests {

    /*
     * 'trimToSize' Tests
     */

    @Test
    fun testTrimToSizeEndSize() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        trimToSize(1)
        size shouldBe 1.asSize()
        maxSize shouldBe MAX_SIZE.asMaxSize()
    }

    @Test
    fun testTrimToSizeElements() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        trimToSize(1)
        get(KEY_1) shouldBe null.asValue()
        get(KEY_2) shouldBe VAL_2.asValue()
    }

    @Test
    fun testTrimToSizeTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        trimToSize(1)

        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe true.asEvicted()
            key shouldBe KEY_1.asKey()
            oldValue shouldBe VAL_1.asOldValue()
            newValue shouldBe null.asValue()
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
        size shouldBe 1.asSize()
        maxSize shouldBe 1.asMaxSize()
    }

    @Test
    fun testResizeElements() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        resize(1)
        get(KEY_1) shouldBe null.asValue()
        get(KEY_2) shouldBe VAL_2.asValue()
    }

    @Test
    fun testResizeTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        resize(1)

        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe true.asEvicted()
            key shouldBe KEY_1.asKey()
            oldValue shouldBe VAL_1.asOldValue()
            newValue shouldBe null.asValue()
        }
    }

    /*
     * Eviction Tests
     */

    @Test
    fun testEviction() = runBasicInMemoryKacheRemoveListenerTest(maxSize = 1) { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)

        size shouldBe 1L.asSize()
        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe true.asEvicted()
            key shouldBe KEY_1.asKey()
            oldValue shouldBe VAL_1.asOldValue()
            newValue shouldBe null.asValue()
        }
    }

    @Test
    fun testEvictionSameAsMaxSize() = runBasicInMemoryKacheRemoveListenerTest(maxSize = 2) { removedEntries ->
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)

        size shouldBe 2.asSize()
        removedEntries shouldHaveSize 0
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

        size shouldBe (VAL_1 + VAL_2).asSize()
    }
}