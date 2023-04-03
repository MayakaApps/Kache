package com.mayakapps.kache

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DeferredResultUnused")
class LruCacheGetTests {

    /*
     * getKeys() tests
     */

    @Test
    fun testGetKeysEmpty() = runBasicLruCacheTest {
        getKeys() shouldBe emptySet()
    }

    @Test
    fun testGetKeysNonEmpty() = runBasicLruCacheTest {
        put(KEY, VAL)
        put(ALT_KEY, ALT_VAL)
        getKeys() shouldBe setOf(KEY, ALT_KEY)
    }

    /*
     * getUnderCreationKeys() tests
     */

    @Test
    fun testGetUnderCreationKeysEmpty() = runBasicLruCacheTest {
        getUnderCreationKeys() shouldBe emptySet()
    }

    @Test
    fun testGetUnderCreationKeysNonEmpty() = runBasicLruCacheTest {
        putAsync(KEY) { VAL }
        putAsync(ALT_KEY) { ALT_VAL }
        getUnderCreationKeys() shouldBe setOf(KEY, ALT_KEY)
    }

    /*
     * getAllKeys() tests
     */

    @Test
    fun testGetAllKeysEmpty() = runBasicLruCacheTest {
        getAllKeys() shouldBe InMemoryKache.Keys(emptySet(), emptySet())
    }

    @Test
    fun testGetAllKeysNonEmpty() = runBasicLruCacheTest {
        put(KEY, VAL)
        putAsync(ALT_KEY) { ALT_VAL }
        getAllKeys() shouldBe InMemoryKache.Keys(setOf(KEY), setOf(ALT_KEY))
    }

    /*
     * getOrDefault tests
     */

    @Test
    fun testGetOrDefaultNonExisting() = runBasicLruCacheTest {
        getOrDefault(KEY, VAL) shouldBe VAL.asValue()
    }

    @Test
    fun testGetOrDefaultExisting() = runBasicLruCacheTest {
        put(KEY, VAL)
        getOrDefault(KEY, ALT_VAL) shouldBe VAL.asValue()
    }

    @Test
    fun testGetOrDefaultCreating() = runBasicLruCacheTest {
        putAsync(KEY) { VAL }
        getOrDefault(KEY, ALT_VAL) shouldBe VAL.asValue()
    }

    /*
     * get tests
     */

    @Test
    fun testGetNonExisting() = runBasicLruCacheTest {
        put(KEY, VAL)
        get(ALT_KEY) shouldBe null.asValue()
    }

    @Test
    fun testGetExisting() = runBasicLruCacheTest {
        put(KEY, VAL)
        get(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testGetCreating() = runBasicLruCacheTest {
        putAsync(KEY) { VAL }
        get(KEY) shouldBe VAL.asValue()
    }

    /*
     * getIfAvailableOrDefault tests
     */

    @Test
    fun testGetIfAvailableOrDefaultNonExisting() = runBasicLruCacheTest {
        getIfAvailableOrDefault(KEY, VAL) shouldBe VAL.asValue()
    }

    @Test
    fun testGetIfAvailableOrDefaultExisting() = runBasicLruCacheTest {
        put(KEY, VAL)
        getIfAvailableOrDefault(KEY, ALT_VAL) shouldBe VAL.asValue()
    }

    @Test
    fun testGetIfAvailableOrDefaultCreating() = runBasicLruCacheTest {
        putAsync(KEY) { VAL }
        getIfAvailableOrDefault(KEY, ALT_VAL) shouldBe ALT_VAL.asValue()
    }

    /*
     * getIfAvailable tests
     */

    @Test
    fun testGetIfAvailableNonExisting() = runBasicLruCacheTest {
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    @Test
    fun testGetIfAvailableExisting() = runBasicLruCacheTest {
        put(KEY, VAL)
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testGetIfAvailableCreating() = runBasicLruCacheTest {
        putAsync(KEY) { VAL }
        getIfAvailable(KEY) shouldBe null.asValue()
    }
}