/*
 * Copyright 2023-2024 MayakaApps
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

import com.mayakapps.kache.MsTimeSource
import kotlin.test.*
import kotlin.time.TimeSource

class ChainTest {

    @Test
    fun forEachIndexed() {
        // Empty chain
        val chain = mutableChainOf()
        assertContentEquals(emptyList(), chain.toIndexListByForEachIndexed())
        assertContentEquals(emptyList(), chain.toIndexListByForEachIndexed(reversed = true))

        // One element
        chain.addToEnd(4)
        assertContentEquals(listOf(4), chain.toIndexListByForEachIndexed())
        assertContentEquals(listOf(4), chain.toIndexListByForEachIndexed(reversed = true))

        // Several elements
        chain.addToEnd(2)
        chain.addToEnd(8)
        assertContentEquals(listOf(4, 2, 8), chain.toIndexListByForEachIndexed())
        assertContentEquals(listOf(8, 2, 4), chain.toIndexListByForEachIndexed(reversed = true))
    }

    @Test
    fun iterator() {
        // Empty chain
        val chain = mutableChainOf()
        assertContentEquals(emptyList(), chain.toIndexListByIterator())
        assertContentEquals(emptyList(), chain.toIndexListByIterator(reversed = true))

        // One element
        chain.addToEnd(4)
        assertContentEquals(listOf(4), chain.toIndexListByIterator())
        assertContentEquals(listOf(4), chain.toIndexListByIterator(reversed = true))

        // Several elements
        chain.addToEnd(2)
        chain.addToEnd(8)
        assertContentEquals(listOf(4, 2, 8), chain.toIndexListByIterator())
        assertContentEquals(listOf(8, 2, 4), chain.toIndexListByIterator(reversed = true))
    }

    @Test
    fun initializeStorage() {
        val chain = mutableChainOf(2, 4, 8)
        chain.initializeStorage(20)
        assertContentEquals(emptyList(), chain.toIndexList())
        assertContentEquals(emptyList(), chain.toIndexList(reversed = true))

        // Make sure that the chain was reinitialized with the new capacity
        try {
            chain.addToEnd(10)
            chain.addToEnd(19)
        } catch (e: IndexOutOfBoundsException) {
            fail("The chain was not reinitialized with the new capacity")
        }
    }

    @Test
    fun resizeStorage() {
        val chain = mutableChainOf(1, 0, 2, initialCapacity = 3)
        chain.resizeStorage(20, listOf(12, 7, 16)::get)
        assertContentEquals(listOf(7, 12, 16), chain.toIndexList())
        assertContentEquals(listOf(16, 12, 7), chain.toIndexList(reversed = true))

        // Make sure that the chain was reinitialized with the new capacity
        try {
            chain.addToEnd(10)
            chain.addToEnd(19)
        } catch (e: IndexOutOfBoundsException) {
            fail("The chain was not reinitialized with the new capacity")
        }
    }

    @Test
    fun resizeStorageWithCalculator() {
        val chain = mutableChainOf(1, 0, 2, initialCapacity = 3)
        val newIndices = listOf(12, 7, 16)
        chain.resizeStorage(20) { newIndices[it] }
        assertContentEquals(listOf(7, 12, 16), chain.toIndexList())
        assertContentEquals(listOf(16, 12, 7), chain.toIndexList(reversed = true))

        // Make sure that the chain was reinitialized with the new capacity
        try {
            chain.addToEnd(10)
            chain.addToEnd(19)
        } catch (e: IndexOutOfBoundsException) {
            fail("The chain was not reinitialized with the new capacity")
        }
    }

    @Test
    fun addToEnd() {
        val chain = mutableChainOf()

        // Add to empty chain
        chain.addToEnd(4)
        assertContentEquals(listOf(4), chain.toIndexList())
        assertContentEquals(listOf(4), chain.toIndexList(reversed = true))

        // Add to non-empty chain
        chain.addToEnd(2)
        chain.addToEnd(8)
        assertContentEquals(listOf(4, 2, 8), chain.toIndexList())
        assertContentEquals(listOf(8, 2, 4), chain.toIndexList(reversed = true))
    }

    @Test
    fun moveToEnd() {
        val chain = mutableChainOf(4, 2, 8)

        // Move the tail
        chain.moveToEnd(8)
        assertContentEquals(listOf(4, 2, 8), chain.toIndexList())
        assertContentEquals(listOf(8, 2, 4), chain.toIndexList(reversed = true))

        // Move a middle element
        chain.moveToEnd(2)
        assertContentEquals(listOf(4, 8, 2), chain.toIndexList())
        assertContentEquals(listOf(2, 8, 4), chain.toIndexList(reversed = true))

        // Move the head
        chain.moveToEnd(4)
        assertContentEquals(listOf(8, 2, 4), chain.toIndexList())
        assertContentEquals(listOf(4, 2, 8), chain.toIndexList(reversed = true))
    }

    @Test
    fun remove() {
        val chain = mutableChainOf(4, 2, 8)

        // Remove the tail
        chain.remove(8)
        assertContentEquals(listOf(4, 2), chain.toIndexList())
        assertContentEquals(listOf(2, 4), chain.toIndexList(reversed = true))
        chain.addToEnd(8)

        // Remove a middle element
        chain.remove(2)
        assertContentEquals(listOf(4, 8), chain.toIndexList())
        assertContentEquals(listOf(8, 4), chain.toIndexList(reversed = true))

        // Remove the head
        chain.remove(4)
        assertContentEquals(listOf(8), chain.toIndexList())
        assertContentEquals(listOf(8), chain.toIndexList(reversed = true))

        // Remove the last element
        chain.remove(8)
        assertContentEquals(emptyList(), chain.toIndexList())
        assertContentEquals(emptyList(), chain.toIndexList(reversed = true))
    }

    @Test
    fun clear() {
        val chain = mutableChainOf(4, 2, 8)
        chain.clear()
        assertContentEquals(emptyList(), chain.toIndexList())
        assertContentEquals(emptyList(), chain.toIndexList(reversed = true))
    }

    @Test
    fun timedChain() {
        val timeSource = MsTimeSource()
        val chain = mutableTimedChainOf(4, 2, 8, timeSource = timeSource)

        timeSource += 1000L

        assertEquals(1000L, chain.getTimeMark(4)!!.elapsedNow().inWholeMilliseconds)
        assertEquals(1000L, chain.getTimeMark(2)!!.elapsedNow().inWholeMilliseconds)
        assertEquals(1000L, chain.getTimeMark(8)!!.elapsedNow().inWholeMilliseconds)

        chain.moveToEnd(2)
        chain.addToEnd(6)

        timeSource += 1000L

        assertEquals(1000L, chain.getTimeMark(2)!!.elapsedNow().inWholeMilliseconds)
        assertEquals(1000L, chain.getTimeMark(6)!!.elapsedNow().inWholeMilliseconds)

        chain.remove(2)
        assertNull(chain.getTimeMark(2))

        chain.clear()
        assertNull(chain.getTimeMark(6))
    }

    private fun mutableChainOf(vararg indices: Int, initialCapacity: Int = 10): MutableChain {
        require(initialCapacity >= indices.size) { "initialCapacity must be >= indices.size" }

        val chain = MutableChain(initialCapacity)
        indices.forEach { chain.addToEnd(it) }
        return chain
    }

    private fun mutableTimedChainOf(
        vararg indices: Int,
        initialCapacity: Int = 10,
        timeSource: TimeSource,
    ): MutableTimedChain {
        require(initialCapacity >= indices.size) { "initialCapacity must be >= indices.size" }

        val chain = MutableTimedChain(initialCapacity, timeSource)
        indices.forEach { chain.addToEnd(it) }
        return chain
    }

    private fun Chain.toIndexList(reversed: Boolean = false): List<Int> =
        toIndexListByForEachIndexed(reversed)

    private fun Chain.toIndexListByForEachIndexed(reversed: Boolean = false): List<Int> {
        val list = mutableListOf<Int>()
        forEachIndexed(reversed) { list += it }
        return list
    }

    private fun Chain.toIndexListByIterator(reversed: Boolean = false): List<Int> {
        val list = mutableListOf<Int>()

        val iterator = object : Chain.AbstractIterator<Int>(this, reversed) {
            override fun getElement(index: Int): Int = index
        }

        while (iterator.hasNext()) {
            list += iterator.next()
        }

        return list
    }
}
