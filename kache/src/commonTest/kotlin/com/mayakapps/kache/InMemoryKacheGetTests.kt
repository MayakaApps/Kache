package com.mayakapps.kache

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DeferredResultUnused")
class InMemoryKacheGetTests {

    /*
     * getKeys() tests
     */

    @Test
    fun testGetKeysEmpty() = runBasicInMemoryKacheTest {
        getKeys() shouldBe emptySet()
    }

    @Test
    fun testGetKeysNonEmpty() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        put(ALT_KEY, ALT_VAL)
        getKeys() shouldBe setOf(KEY, ALT_KEY)
    }

    /*
     * getUnderCreationKeys() tests
     */

    @Test
    fun testGetUnderCreationKeysEmpty() = runBasicInMemoryKacheTest {
        getUnderCreationKeys() shouldBe emptySet()
    }

    @Test
    fun testGetUnderCreationKeysNonEmpty() = runBasicInMemoryKacheTest {
        putAsync(KEY) { VAL }
        putAsync(ALT_KEY) { ALT_VAL }
        getUnderCreationKeys() shouldBe setOf(KEY, ALT_KEY)
    }

    /*
     * getAllKeys() tests
     */

    @Test
    fun testGetAllKeysEmpty() = runBasicInMemoryKacheTest {
        getAllKeys() shouldBe InMemoryKache.Keys(emptySet(), emptySet())
    }

    @Test
    fun testGetAllKeysNonEmpty() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        putAsync(ALT_KEY) { ALT_VAL }
        getAllKeys() shouldBe InMemoryKache.Keys(setOf(KEY), setOf(ALT_KEY))
    }

    /*
     * getOrDefault tests
     */

    @Test
    fun testGetOrDefaultNonExisting() = runBasicInMemoryKacheTest {
        getOrDefault(KEY, VAL) shouldBe VAL.asValue()
    }

    @Test
    fun testGetOrDefaultExisting() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        getOrDefault(KEY, ALT_VAL) shouldBe VAL.asValue()
    }

    @Test
    fun testGetOrDefaultCreating() = runBasicInMemoryKacheTest {
        putAsync(KEY) { VAL }
        getOrDefault(KEY, ALT_VAL) shouldBe VAL.asValue()
    }

    /*
     * get tests
     */

    @Test
    fun testGetNonExisting() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        get(ALT_KEY) shouldBe null.asValue()
    }

    @Test
    fun testGetExisting() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        get(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testGetCreating() = runBasicInMemoryKacheTest {
        putAsync(KEY) { VAL }
        get(KEY) shouldBe VAL.asValue()
    }

    /*
     * getIfAvailableOrDefault tests
     */

    @Test
    fun testGetIfAvailableOrDefaultNonExisting() = runBasicInMemoryKacheTest {
        getIfAvailableOrDefault(KEY, VAL) shouldBe VAL.asValue()
    }

    @Test
    fun testGetIfAvailableOrDefaultExisting() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        getIfAvailableOrDefault(KEY, ALT_VAL) shouldBe VAL.asValue()
    }

    @Test
    fun testGetIfAvailableOrDefaultCreating() = runBasicInMemoryKacheTest {
        putAsync(KEY) { VAL }
        getIfAvailableOrDefault(KEY, ALT_VAL) shouldBe ALT_VAL.asValue()
    }

    /*
     * getIfAvailable tests
     */

    @Test
    fun testGetIfAvailableNonExisting() = runBasicInMemoryKacheTest {
        getIfAvailable(KEY) shouldBe null.asValue()
    }

    @Test
    fun testGetIfAvailableExisting() = runBasicInMemoryKacheTest {
        put(KEY, VAL)
        getIfAvailable(KEY) shouldBe VAL.asValue()
    }

    @Test
    fun testGetIfAvailableCreating() = runBasicInMemoryKacheTest {
        putAsync(KEY) { VAL }
        getIfAvailable(KEY) shouldBe null.asValue()
    }
}