/*
 * Copyright 2023 MayakaApps
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
