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

package com.mayakapps.kache.collection

import kotlin.test.*

class LinkedScatterMapTest {
    @Test
    fun scatterMap() {
        val map = MutableLinkedScatterMap<String, String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableLinkedScatterMap<String, String>(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun scatterMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableLinkedScatterMap<String, String>(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun addToMap() {
        val map = MutableLinkedScatterMap<String, String>()
        map["Hello"] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map["Hello"])
    }

    @Test
    fun insertIndex0() {
        val map = MutableLinkedScatterMap<Float, Long>()
        map.put(1f, 100L)
        assertEquals(100L, map[1f])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableLinkedScatterMap<String, String>(12)
        map["Hello"] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map["Hello"])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableLinkedScatterMap<String, String>(2)
        map["Hello"] = "World"

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals("World", map["Hello"])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableLinkedScatterMap<String, String>(0)
        map["Hello"] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map["Hello"])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableLinkedScatterMap<String, String>()
        map["Hello"] = "World"
        map["Hello"] = "Monde"

        assertEquals(1, map.size)
        assertEquals("Monde", map["Hello"])
    }

    @Test
    fun put() {
        val map = MutableLinkedScatterMap<String, String?>()

        assertNull(map.put("Hello", "World"))
        assertEquals("World", map.put("Hello", "Monde"))
        assertNull(map.put("Bonjour", null))
        assertNull(map.put("Bonjour", "Monde"))
    }

    @Test
    fun putAllMap() {
        val map = MutableLinkedScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        map.putAll(mapOf("Hallo" to "Welt", "Hola" to "Mundo"))

        assertEquals(5, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun putAllArray() {
        val map = MutableLinkedScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        map.putAll(arrayOf("Hallo" to "Welt", "Hola" to "Mundo"))

        assertEquals(5, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun putAllIterable() {
        val map = MutableLinkedScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        map.putAll(listOf("Hallo" to "Welt", "Hola" to "Mundo"))

        assertEquals(5, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun putAllSequence() {
        val map = MutableLinkedScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        map.putAll(listOf("Hallo" to "Welt", "Hola" to "Mundo").asSequence())

        assertEquals(5, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun plus() {
        val map = MutableLinkedScatterMap<String, String>()
        map += "Hello" to "World"

        assertEquals(1, map.size)
        assertEquals("World", map["Hello"])
    }

    @Test
    fun plusMap() {
        val map = MutableLinkedScatterMap<String, String>()
        map += mapOf("Hallo" to "Welt", "Hola" to "Mundo")

        assertEquals(2, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun plusArray() {
        val map = MutableLinkedScatterMap<String, String>()
        map += arrayOf("Hallo" to "Welt", "Hola" to "Mundo")

        assertEquals(2, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun plusIterable() {
        val map = MutableLinkedScatterMap<String, String>()
        map += listOf("Hallo" to "Welt", "Hola" to "Mundo")

        assertEquals(2, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun plusSequence() {
        val map = MutableLinkedScatterMap<String, String>()
        map += listOf("Hallo" to "Welt", "Hola" to "Mundo").asSequence()

        assertEquals(2, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun nullKey() {
        val map = MutableLinkedScatterMap<String?, String>()
        map[null] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map[null])
    }

    @Test
    fun nullValue() {
        val map = MutableLinkedScatterMap<String, String?>()
        map["Hello"] = null

        assertEquals(1, map.size)
        assertNull(map["Hello"])
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableLinkedScatterMap<String, String?>()
        map["Hello"] = "World"

        assertNull(map["Bonjour"])
    }

    @Test
    fun getOrDefault() {
        val map = MutableLinkedScatterMap<String, String?>()
        map["Hello"] = "World"

        assertEquals("Monde", map.getOrDefault("Bonjour", "Monde"))
    }

    @Test
    fun getOrElse() {
        val map = MutableLinkedScatterMap<String, String?>()
        map["Hello"] = "World"
        map["Bonjour"] = null

        assertEquals("Monde", map.getOrElse("Bonjour") { "Monde" })
        assertEquals("Welt", map.getOrElse("Hallo") { "Welt" })
    }

    @Test
    fun getOrPut() {
        val map = MutableLinkedScatterMap<String, String?>()
        map["Hello"] = "World"

        var counter = 0
        map.getOrPut("Hello") {
            counter++
            "Monde"
        }
        assertEquals("World", map["Hello"])
        assertEquals(0, counter)

        map.getOrPut("Bonjour") {
            counter++
            "Monde"
        }
        assertEquals("Monde", map["Bonjour"])
        assertEquals(1, counter)

        map.getOrPut("Bonjour") {
            counter++
            "Welt"
        }
        assertEquals("Monde", map["Bonjour"])
        assertEquals(1, counter)

        map.getOrPut("Hallo") {
            counter++
            null
        }
        assertNull(map["Hallo"])
        assertEquals(2, counter)

        map.getOrPut("Hallo") {
            counter++
            "Welt"
        }
        assertEquals("Welt", map["Hallo"])
        assertEquals(3, counter)
    }

    @Test
    fun compute() {
        val map = MutableLinkedScatterMap<String, String?>()
        map["Hello"] = "World"

        var computed = map.compute("Hello") { _, _ ->
            "New World"
        }
        assertEquals("New World", map["Hello"])
        assertEquals("New World", computed)

        computed = map.compute("Bonjour") { _, _ ->
            "Monde"
        }
        assertEquals("Monde", map["Bonjour"])
        assertEquals("Monde", computed)

        map.compute("Bonjour") { _, v ->
            v ?: "Welt"
        }
        assertEquals("Monde", map["Bonjour"])

        map.compute("Hallo") { _, _ ->
            null
        }
        assertNull(map["Hallo"])

        map.compute("Hallo") { _, v ->
            v ?: "Welt"
        }
        assertEquals("Welt", map["Hallo"])
    }

    @Test
    fun remove() {
        val map = MutableLinkedScatterMap<String?, String?>()
        assertNull(map.remove("Hello"))

        map["Hello"] = "World"
        assertEquals("World", map.remove("Hello"))
        assertEquals(0, map.size)

        map[null] = "World"
        assertEquals("World", map.remove(null))
        assertEquals(0, map.size)

        map["Hello"] = null
        assertNull(map.remove("Hello"))
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableLinkedScatterMap<String, String>(6)
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        map["Konnichiwa"] = "Sekai"
        map["Ciao"] = "Mondo"
        map["Annyeong"] = "Sesang"

        // Removing all the entries will mark the metadata as deleted
        map.remove("Hello")
        map.remove("Bonjour")
        map.remove("Hallo")
        map.remove("Konnichiwa")
        map.remove("Ciao")
        map.remove("Annyeong")

        assertEquals(0, map.size)

        val capacity = map.capacity

        // Make sure reinserting an entry after filling the table
        // with "Deleted" markers works
        map["Hello"] = "World"

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableLinkedScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        map["Konnichiwa"] = "Sekai"
        map["Ciao"] = "Mondo"
        map["Annyeong"] = "Sesang"

        map.removeIf { key, value -> key.startsWith('H') || value.startsWith('S') }

        assertEquals(2, map.size)
        assertEquals("Monde", map["Bonjour"])
        assertEquals("Mondo", map["Ciao"])
    }

    @Test
    fun minus() {
        val map = MutableLinkedScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        map -= "Hello"

        assertEquals(2, map.size)
        assertNull(map["Hello"])
    }

    @Test
    fun minusArray() {
        val map = MutableLinkedScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        map -= arrayOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertNull(map["Hallo"])
        assertNull(map["Bonjour"])
    }

    @Test
    fun minusIterable() {
        val map = MutableLinkedScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        map -= listOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertNull(map["Hallo"])
        assertNull(map["Bonjour"])
    }

    @Test
    fun minusSequence() {
        val map = MutableLinkedScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        map -= listOf("Hallo", "Bonjour").asSequence()

        assertEquals(1, map.size)
        assertNull(map["Hallo"])
        assertNull(map["Bonjour"])
    }

    @Test
    fun minusObjectList() {
        val map = MutableLinkedScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        map -= objectListOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertNull(map["Hallo"])
        assertNull(map["Bonjour"])
    }

    @Test
    fun conditionalRemove() {
        val map = MutableLinkedScatterMap<String?, String?>()
        assertFalse(map.remove("Hello", "World"))

        map["Hello"] = "World"
        assertTrue(map.remove("Hello", "World"))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableLinkedScatterMap<String, String>()

        for (i in 0 until 1700) {
            val s = i.toString()
            map[s] = s
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableLinkedScatterMap<String, String>()

            for (j in 0 until i) {
                val s = j.toString()
                map[s] = s
            }

            var counter = 0
            map.forEach { key, value ->
                assertEquals(key, value)
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachKey() {
        for (i in 0..48) {
            val map = MutableLinkedScatterMap<String, String>()

            for (j in 0 until i) {
                val s = j.toString()
                map[s] = s
            }

            var counter = 0
            map.forEachKey { key ->
                assertNotNull(key.toIntOrNull())
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachValue() {
        for (i in 0..48) {
            val map = MutableLinkedScatterMap<String, String>()

            for (j in 0 until i) {
                val s = j.toString()
                map[s] = s
            }

            var counter = 0
            map.forEachValue { value ->
                assertNotNull(value.toIntOrNull())
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun clear() {
        val map = MutableLinkedScatterMap<String, String>()

        for (i in 0 until 32) {
            val s = i.toString()
            map[s] = s
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableLinkedScatterMap<String?, String?>()
        assertEquals("{}", map.toString())

        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        assertTrue(
            "{Hello=World, Bonjour=Monde}" == map.toString() ||
                    "{Bonjour=Monde, Hello=World}" == map.toString()
        )

        map.clear()
        map["Hello"] = null
        assertEquals("{Hello=null}", map.toString())

        map.clear()
        map[null] = "Monde"
        assertEquals("{null=Monde}", map.toString())

        val selfAsKeyMap = MutableLinkedScatterMap<Any, String>()
        selfAsKeyMap[selfAsKeyMap] = "Hello"
        assertEquals("{(this)=Hello}", selfAsKeyMap.toString())

        val selfAsValueMap = MutableLinkedScatterMap<String, Any>()
        selfAsValueMap["Hello"] = selfAsValueMap
        assertEquals("{Hello=(this)}", selfAsValueMap.toString())

        // Test with a small map
        val map2 = MutableLinkedScatterMap<String?, String?>(2)
        map2["Hello"] = "World"
        map2["Bonjour"] = "Monde"
        assertTrue(
            "{Hello=World, Bonjour=Monde}" == map2.toString() ||
                    "{Bonjour=Monde, Hello=World}" == map2.toString()
        )
    }

    @Test
    fun joinToString() {
        val map = MutableLinkedScatterMap<Int, Float>()
            .apply { putAll(arrayOf(1 to 1f, 2 to 2f, 3 to 3f, 4 to 4f, 5 to 5f)) }
        val order = IntArray(5)
        var index = 0
        map.forEach { key, _ ->
            order[index++] = key
        }
        assertEquals(
            "${order[0]}=${order[0].toFloat()}, ${order[1]}=${order[1].toFloat()}, " +
                    "${order[2]}=${order[2].toFloat()}, ${order[3]}=${order[3].toFloat()}, " +
                    "${order[4]}=${order[4].toFloat()}",
            map.joinToString()
        )
        assertEquals(
            "x${order[0]}=${order[0].toFloat()}, ${order[1]}=${order[1].toFloat()}, " +
                    "${order[2]}=${order[2].toFloat()}...",
            map.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0]}=${order[0].toFloat()}-${order[1]}=${order[1].toFloat()}-" +
                    "${order[2]}=${order[2].toFloat()}-${order[3]}=${order[3].toFloat()}-" +
                    "${order[4]}=${order[4].toFloat()}<",
            map.joinToString(separator = "-", prefix = ">", postfix = "<")
        )
        val names = arrayOf("one", "two", "three", "four", "five")
        assertEquals(
            "${names[order[0]]}, ${names[order[1]]}, ${names[order[2]]}...",
            map.joinToString(limit = 3) { key, _ -> names[key] }
        )
    }

    @Test
    fun equalsTest() {
        val map = MutableLinkedScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableLinkedScatterMap<String?, String?>()
        map2["Bonjour"] = null
        map2[null] = "Monde"

        assertNotEquals(map, map2)

        map2["Hello"] = "World"
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableLinkedScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        assertTrue(map.containsKey("Hello"))
        assertTrue(map.containsKey(null))
        assertFalse(map.containsKey("World"))
    }

    @Test
    fun contains() {
        val map = MutableLinkedScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        assertTrue("Hello" in map)
        assertTrue(null in map)
        assertFalse("World" in map)
    }

    @Test
    fun containsValue() {
        val map = MutableLinkedScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        assertTrue(map.containsValue("World"))
        assertTrue(map.containsValue(null))
        assertFalse(map.containsValue("Hello"))
    }

    @Test
    fun empty() {
        val map = MutableLinkedScatterMap<String?, String?>()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map["Hello"] = "World"

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableLinkedScatterMap<String, String>()
        assertEquals(0, map.count())

        map["Hello"] = "World"
        assertEquals(1, map.count())

        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        map["Konnichiwa"] = "Sekai"
        map["Ciao"] = "Mondo"
        map["Annyeong"] = "Sesang"

        assertEquals(2, map.count { key, _ -> key.startsWith("H") })
        assertEquals(0, map.count { key, _ -> key.startsWith("W") })
    }

    @Test
    fun any() {
        val map = MutableLinkedScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        map["Konnichiwa"] = "Sekai"
        map["Ciao"] = "Mondo"
        map["Annyeong"] = "Sesang"

        assertTrue(map.any { key, _ -> key.startsWith("K") })
        assertFalse(map.any { key, _ -> key.startsWith("W") })
    }

    @Test
    fun all() {
        val map = MutableLinkedScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        map["Konnichiwa"] = "Sekai"
        map["Ciao"] = "Mondo"
        map["Annyeong"] = "Sesang"

        assertTrue(map.any { key, value -> key.length >= 5 && value.length >= 4 })
        assertFalse(map.all { key, _ -> key.startsWith("W") })
    }

    @Test
    fun trim() {
        val map = MutableLinkedScatterMap<String, String>()
        assertEquals(7, map.trim())

        map["Hello"] = "World"
        map["Hallo"] = "Welt"

        assertEquals(0, map.trim())

        for (i in 0 until 1700) {
            val s = i.toString()
            map[s] = s
        }

        assertEquals(2047, map.capacity)

        // After removing these items, our capacity needs should go
        // from 2047 down to 1023
        for (i in 0 until 1700) {
            if (i and 0x1 == 0x0) {
                val s = i.toString()
                map.remove(s)
            }
        }

        assertEquals(1024, map.trim())
        assertEquals(0, map.trim())
    }
}
