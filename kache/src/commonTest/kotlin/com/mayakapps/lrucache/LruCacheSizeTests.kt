package com.mayakapps.lrucache

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LruCacheSizeTests {

    /*
     * 'trimToSize' Tests
     */

    @Test
    fun testTrimToSizeEndSize() = runBasicLruCacheTest {
        put(KEY, VAL)
        put(ALT_KEY, ALT_VAL)
        trimToSize(1)
        size shouldBe 1.asSize()
        maxSize shouldBe MAX_SIZE.asMaxSize()
    }

    @Test
    fun testTrimToSizeElements() = runBasicLruCacheTest {
        put(KEY, VAL)
        put(ALT_KEY, ALT_VAL)
        trimToSize(1)
        get(KEY) shouldBe null.asValue()
        get(ALT_KEY) shouldBe ALT_VAL.asValue()
    }

    @Test
    fun testTrimToSizeTrigger() = runBasicLruCacheRemoveListenerTest { removedEntries ->
        put(KEY, VAL)
        put(ALT_KEY, ALT_VAL)
        trimToSize(1)

        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe true.asEvicted()
            key shouldBe KEY.asKey()
            oldValue shouldBe VAL.asOldValue()
            newValue shouldBe null.asValue()
        }
    }

    /*
     * 'resize' Tests
     */

    @Test
    fun testResizeEndSize() = runBasicLruCacheTest {
        put(KEY, VAL)
        put(ALT_KEY, ALT_VAL)
        resize(1)
        size shouldBe 1.asSize()
        maxSize shouldBe 1.asMaxSize()
    }

    @Test
    fun testResizeElements() = runBasicLruCacheTest {
        put(KEY, VAL)
        put(ALT_KEY, ALT_VAL)
        resize(1)
        get(KEY) shouldBe null.asValue()
        get(ALT_KEY) shouldBe ALT_VAL.asValue()
    }

    @Test
    fun testResizeTrigger() = runBasicLruCacheRemoveListenerTest { removedEntries ->
        put(KEY, VAL)
        put(ALT_KEY, ALT_VAL)
        resize(1)

        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe true.asEvicted()
            key shouldBe KEY.asKey()
            oldValue shouldBe VAL.asOldValue()
            newValue shouldBe null.asValue()
        }
    }

    /*
     * Eviction Tests
     */

    @Test
    fun testEviction() = runBasicLruCacheRemoveListenerTest(maxSize = 1) { removedEntries ->
        put(KEY, VAL)
        put(ALT_KEY, ALT_VAL)

        size shouldBe 1L.asSize()
        removedEntries shouldHaveSize 1
        removedEntries.firstOrNull()?.run {
            evicted shouldBe true.asEvicted()
            key shouldBe KEY.asKey()
            oldValue shouldBe VAL.asOldValue()
            newValue shouldBe null.asValue()
        }
    }

    @Test
    fun testEvictionSameAsMaxSize() = runBasicLruCacheRemoveListenerTest(maxSize = 2) { removedEntries ->
        put(KEY, VAL)
        put(ALT_KEY, ALT_VAL)

        size shouldBe 2.asSize()
        removedEntries shouldHaveSize 0
    }

    /*
     * Size Calculator Tests
     */

    @Test
    fun testSizeCalculator() = runBasicLruCacheTest(
        maxSize = 1L + VAL + ALT_VAL,
        sizeCalculator = { _, value -> value.toLong() },
    ) {
        put(KEY, VAL)
        put(ALT_KEY, ALT_VAL)

        size shouldBe (VAL + ALT_VAL).asSize()
    }
}