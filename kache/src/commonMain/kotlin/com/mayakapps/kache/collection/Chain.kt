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

import com.mayakapps.kache.collection.internal.EMPTY_INTS
import kotlin.jvm.JvmField
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal sealed class Chain {

    @JvmField
    internal var head = -1

    @JvmField
    protected var tail = -1

    @JvmField
    protected var next = EMPTY_INTS

    @JvmField
    protected var prev = EMPTY_INTS

    internal inline fun forEachIndexed(reversed: Boolean = false, action: (index: Int) -> Unit) {
        val iterationHead: Int
        val iterationNext: IntArray
        if (!reversed) {
            iterationHead = head
            iterationNext = next
        } else {
            iterationHead = tail
            iterationNext = prev
        }

        var index = iterationHead
        while (index != -1) {
            val nextIndex = iterationNext[index]
            action(index)
            index = nextIndex
        }
    }
}

internal open class MutableChain(initialCapacity: Int = 0) : Chain() {

    init {
        require(initialCapacity >= 0) { "Capacity must be a positive value." }
        if (initialCapacity > 0) {
            next = IntArray(initialCapacity) { -1 }
            prev = IntArray(initialCapacity) { -1 }
        }
    }

    protected open val extras: Array<Any?>? = null

    internal open fun initializeStorage(capacity: Int) {
        head = -1
        tail = -1
        next = IntArray(capacity) { -1 }
        prev = IntArray(capacity) { -1 }
    }

    internal inline fun resizeStorage(newCapacity: Int, calculateNewIndex: (Int) -> Int) {
        // Capture the current state of the chain.
        val oldHead = head
        val oldNext = next
        val oldExtras = extras

        // Initialize the storage with the new capacity.
        initializeStorage(newCapacity)

        // If the chain was empty, return as there is nothing to migrate.
        if (oldHead == -1) return

        // Initialize iterators. No previous index for the first item. The first item is the head.
        // Set the head to the first item.
        var newPrevIndex = -1
        var oldIndex = oldHead
        var newIndex = calculateNewIndex(oldHead)
        head = newIndex

        while (oldIndex != -1) {
            // Set the old and new next indices.
            val oldNextIndex = oldNext[oldIndex]
            val newNextIndex = if (oldNextIndex >= 0) calculateNewIndex(oldNextIndex) else -1

            // Set the next and previous indices of the current item.
            next[newIndex] = newNextIndex
            prev[newIndex] = newPrevIndex

            // Migrate the extra data if it exists.
            extras?.let { it[newIndex] = oldExtras!![oldIndex] }

            // Set the previous item to the current item and the current item to the next item.
            newPrevIndex = newIndex
            oldIndex = oldNextIndex
            newIndex = newNextIndex
        }

        // Set the tail to the last item.
        tail = newPrevIndex
    }

    internal open fun addToEnd(index: Int) {
        // If the chain is empty, set the head and tail to the current item.
        if (head == -1) {
            head = index
            tail = index
            return
        }

        // Add the item to the end of the chain.
        next[tail] = index
        prev[index] = tail

        // Set the item as the tail of the chain.
        tail = index
    }

    internal open fun moveToEnd(index: Int) {
        // If the item is already at the end, return.
        if (index == tail) return

        // Capture the next and previous indices before modifying the chain.
        val nextIndex = next[index]
        val prevIndex = prev[index]

        // If the item is the head, move the head to the next item.
        if (index == head) head = nextIndex

        // Remove the current item from the chain. This involves updating the next and previous indices of the previous
        // and next items, respectively.
        if (prevIndex != -1) next[prevIndex] = nextIndex
        if (nextIndex != -1) prev[nextIndex] = prevIndex

        // Indicate that there is no next item.
        next[index] = -1

        // Add the item to the end of the chain.
        next[tail] = index
        prev[index] = tail

        // Set the item as the tail of the chain.
        tail = index
    }

    internal open fun remove(index: Int) {
        // Capture the next and previous indices before modifying the chain.
        val nextIndex = next[index]
        val prevIndex = prev[index]

        // If the item is the head, move the head to the next item. If the item is the tail, move the tail to the
        // previous item.
        if (index == head) head = nextIndex
        if (index == tail) tail = prevIndex

        // Remove the current item from the chain. This involves updating the next and previous indices of the previous
        // and next items, respectively.
        if (prevIndex != -1) next[prevIndex] = nextIndex
        if (nextIndex != -1) prev[nextIndex] = prevIndex

        // Clear the info of the current item.
        next[index] = -1
        prev[index] = -1
        extras?.let { it[index] = null }
    }

    internal open fun clear() {
        head = -1
        tail = -1
        next.fill(-1)
        prev.fill(-1)
        extras?.fill(null)
    }
}

internal class MutableTimedChain(
    initialCapacity: Int = 0,
    private val timeSource: TimeSource = TimeSource.Monotonic,
): MutableChain(initialCapacity) {

    private var timeMarks = Array<TimeMark?>(initialCapacity) { null }

    @Suppress("UNCHECKED_CAST")
    override val extras get() = timeMarks as Array<Any?>

    override fun initializeStorage(capacity: Int) {
        super.initializeStorage(capacity)
        timeMarks = Array(capacity) { null }
    }

    internal fun getTimeMark(index: Int): TimeMark = timeMarks[index]
        ?: throw NoSuchElementException("No time mark for the index $index")

    override fun addToEnd(index: Int) {
        super.addToEnd(index)
        timeMarks[index] = timeSource.markNow()
    }

    override fun moveToEnd(index: Int) {
        super.moveToEnd(index)
        timeMarks[index] = timeSource.markNow()
    }
}
