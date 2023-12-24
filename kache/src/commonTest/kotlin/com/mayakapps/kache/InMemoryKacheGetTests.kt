package com.mayakapps.kache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Suppress("DeferredResultUnused")
class InMemoryKacheGetTests {

    /*
     * getKeys() tests
     */

    @Test
    fun testGetKeysEmpty() = runBasicInMemoryKacheTest {
        assertEquals(emptySet(), getKeys())
    }

    @Test
    fun testGetKeysNonEmpty() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        put(KEY_2, VAL_2)
        assertEquals(setOf(KEY_1, KEY_2), getKeys())
    }

    /*
     * getUnderCreationKeys() tests
     */

    @Test
    fun testGetUnderCreationKeysEmpty() = runBasicInMemoryKacheTest {
        assertEquals(emptySet(), getUnderCreationKeys())
    }

    @Test
    fun testGetUnderCreationKeysNonEmpty() = runBasicInMemoryKacheTest {
        putAsync(KEY_1) { VAL_1 }
        putAsync(KEY_2) { VAL_2 }
        assertEquals(setOf(KEY_1, KEY_2), getUnderCreationKeys())
    }

    /*
     * getAllKeys() tests
     */

    @Test
    fun testGetAllKeysEmpty() = runBasicInMemoryKacheTest {
        assertEquals(KacheKeys(emptySet(), emptySet()), getAllKeys())
    }

    @Test
    fun testGetAllKeysNonEmpty() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        putAsync(KEY_2) { VAL_2 }
        assertEquals(KacheKeys(setOf(KEY_1), setOf(KEY_2)), getAllKeys())
    }

    /*
     * getOrDefault tests
     */

    @Test
    fun testGetOrDefaultNonExisting() = runBasicInMemoryKacheTest {
        assertEquals(VAL_1, getOrDefault(KEY_1, VAL_1))
    }

    @Test
    fun testGetOrDefaultExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        assertEquals(VAL_1, getOrDefault(KEY_1, VAL_2))
    }

    @Test
    fun testGetOrDefaultCreating() = runBasicInMemoryKacheTest {
        putAsync(KEY_1) { VAL_1 }
        assertEquals(VAL_1, getOrDefault(KEY_1, VAL_2))
    }

    /*
     * get tests
     */

    @Test
    fun testGetNonExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        assertNull(get(KEY_2))
    }

    @Test
    fun testGetExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        assertEquals(VAL_1, get(KEY_1))
    }

    @Test
    fun testGetCreating() = runBasicInMemoryKacheTest {
        putAsync(KEY_1) { VAL_1 }
        assertEquals(VAL_1, get(KEY_1))
    }

    /*
     * getIfAvailableOrDefault tests
     */

    @Test
    fun testGetIfAvailableOrDefaultNonExisting() = runBasicInMemoryKacheTest {
        assertEquals(VAL_1, getIfAvailableOrDefault(KEY_1, VAL_1))
    }

    @Test
    fun testGetIfAvailableOrDefaultExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        assertEquals(VAL_1, getIfAvailableOrDefault(KEY_1, VAL_2))
    }

    @Test
    fun testGetIfAvailableOrDefaultCreating() = runBasicInMemoryKacheTest {
        putAsync(KEY_1) { VAL_1 }
        assertEquals(VAL_2, getIfAvailableOrDefault(KEY_1, VAL_2))
    }

    /*
     * getIfAvailable tests
     */

    @Test
    fun testGetIfAvailableNonExisting() = runBasicInMemoryKacheTest {
        assertNull(getIfAvailable(KEY_1))
    }

    @Test
    fun testGetIfAvailableExisting() = runBasicInMemoryKacheTest {
        put(KEY_1, VAL_1)
        assertEquals(VAL_1, getIfAvailable(KEY_1))
    }

    @Test
    fun testGetIfAvailableCreating() = runBasicInMemoryKacheTest {
        putAsync(KEY_1) { VAL_1 }
        assertNull(getIfAvailable(KEY_1))
    }
}
