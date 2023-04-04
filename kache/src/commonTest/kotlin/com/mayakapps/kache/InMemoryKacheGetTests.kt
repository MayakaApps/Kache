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
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        getKeys() shouldBe setOf(KEY_1, KEY_2)
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
        putAsync(KEY_1) { VAL_1 }
        putAsync(KEY_2) { VAL_2 }
        getUnderCreationKeys() shouldBe setOf(KEY_1, KEY_2)
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
        put(KEY_1, VAL_1)
        putAsync(KEY_2) { VAL_2 }
        getAllKeys() shouldBe InMemoryKache.Keys(setOf(KEY_1), setOf(KEY_2))
    }

    /*
     * getOrDefault tests
     */

    @Test
    fun testGetOrDefaultNonExisting() = runBasicInMemoryKacheTest {
        getOrDefault(KEY_1, VAL_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testGetOrDefaultExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        getOrDefault(KEY_1, VAL_2) shouldBe VAL_1.asValue()
    }

    @Test
    fun testGetOrDefaultCreating() = runBasicInMemoryKacheTest {
        putAsync(KEY_1) { VAL_1 }
        getOrDefault(KEY_1, VAL_2) shouldBe VAL_1.asValue()
    }

    /*
     * get tests
     */

    @Test
    fun testGetNonExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        get(KEY_2) shouldBe null.asValue()
    }

    @Test
    fun testGetExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        get(KEY_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testGetCreating() = runBasicInMemoryKacheTest {
        putAsync(KEY_1) { VAL_1 }
        get(KEY_1) shouldBe VAL_1.asValue()
    }

    /*
     * getIfAvailableOrDefault tests
     */

    @Test
    fun testGetIfAvailableOrDefaultNonExisting() = runBasicInMemoryKacheTest {
        getIfAvailableOrDefault(KEY_1, VAL_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testGetIfAvailableOrDefaultExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        getIfAvailableOrDefault(KEY_1, VAL_2) shouldBe VAL_1.asValue()
    }

    @Test
    fun testGetIfAvailableOrDefaultCreating() = runBasicInMemoryKacheTest {
        putAsync(KEY_1) { VAL_1 }
        getIfAvailableOrDefault(KEY_1, VAL_2) shouldBe VAL_2.asValue()
    }

    /*
     * getIfAvailable tests
     */

    @Test
    fun testGetIfAvailableNonExisting() = runBasicInMemoryKacheTest {
        getIfAvailable(KEY_1) shouldBe null.asValue()
    }

    @Test
    fun testGetIfAvailableExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        getIfAvailable(KEY_1) shouldBe VAL_1.asValue()
    }

    @Test
    fun testGetIfAvailableCreating() = runBasicInMemoryKacheTest {
        putAsync(KEY_1) { VAL_1 }
        getIfAvailable(KEY_1) shouldBe null.asValue()
    }
}