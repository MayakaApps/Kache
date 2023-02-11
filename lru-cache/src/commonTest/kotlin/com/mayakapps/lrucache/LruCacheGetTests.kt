package com.mayakapps.lrucache

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DeferredResultUnused")
class LruCacheGetTests {

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