package com.mayakapps.kache

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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
    fun testTrimToSizeElementsForLRU() = runBasicInMemoryKacheTest(strategy = KacheStrategy.LRU) {
        prepareKache()
        trimToSize(3)
        get(KEY_1) shouldBe VAL_1.asValue()
        get(KEY_2) shouldBe VAL_2.asValue()
        get(KEY_3) shouldBe null.asValue()
        get(KEY_4) shouldBe VAL_4.asValue()
    }

    @Test
    fun testTrimToSizeElementsForMRU() = runBasicInMemoryKacheTest(strategy = KacheStrategy.MRU) {
        prepareKache()
        trimToSize(3)
        get(KEY_1) shouldBe VAL_1.asValue()
        get(KEY_2) shouldBe null.asValue()
        get(KEY_3) shouldBe VAL_3.asValue()
        get(KEY_4) shouldBe VAL_4.asValue()
    }

    @Test
    fun testTrimToSizeElementsForFIFO() = runBasicInMemoryKacheTest(strategy = KacheStrategy.FIFO) {
        prepareKache()
        trimToSize(3)
        get(KEY_1) shouldBe null.asValue()
        get(KEY_2) shouldBe VAL_2.asValue()
        get(KEY_3) shouldBe VAL_3.asValue()
        get(KEY_4) shouldBe VAL_4.asValue()
    }

    @Test
    fun testTrimToSizeElementsForFILO() = runBasicInMemoryKacheTest(strategy = KacheStrategy.FILO) {
        prepareKache()
        trimToSize(3)
        get(KEY_1) shouldBe VAL_1.asValue()
        get(KEY_2) shouldBe VAL_2.asValue()
        get(KEY_3) shouldBe VAL_3.asValue()
        get(KEY_4) shouldBe null.asValue()
    }

    @Test
    fun testTrimToSizeTrigger() = runBasicInMemoryKacheRemoveListenerTest { removedEntries ->
        prepareKache()
        trimToSize(3)

        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe true.asEvicted()
            key shouldBe KEY_3.asKey()
            oldValue shouldBe VAL_3.asOldValue()
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
