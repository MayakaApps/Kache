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
    internal var tail = -1

    @JvmField
    internal var next = EMPTY_INTS

    @JvmField
    internal var prev = EMPTY_INTS

    internal inline fun forEachIndexed(reversed: Boolean = false, action: (index: Int) -> Unit) {
        if (reversed) {
            iterateChain(tail, prev, action)
        } else {
            iterateChain(head, next, action)
        }
    }

    internal abstract class AbstractIterator<T>(
        private val parent: Chain,
        private val reversed: Boolean,
    ) : Iterator<T> {

        private var nextIndex = if (reversed) parent.tail else parent.head

        override fun hasNext(): Boolean = nextIndex != -1

        override fun next(): T {
            if (nextIndex == -1) {
                throw NoSuchElementException()
            }

            val index = nextIndex
            nextIndex = if (reversed) parent.prev[index] else parent.next[index]
            return getElement(index)
        }

        protected abstract fun getElement(index: Int): T
    }
}

internal open class MutableChain(initialCapacity: Int) : Chain() {

    init {
        require(initialCapacity >= 0) { "Capacity must be a positive value." }
        if (initialCapacity > 0) {
            next = IntArray(initialCapacity) { -1 }
            prev = IntArray(initialCapacity) { -1 }
        }
    }

    internal open fun initializeStorage(capacity: Int) {
        head = -1
        tail = -1
        next = IntArray(capacity) { -1 }
        prev = IntArray(capacity) { -1 }
    }

    internal fun resizeStorage(newCapacity: Int, newIndices: IntArray) {
        val oldHead = head
        val oldNext = next
        val oldExtras = getExtras()

        initializeStorage(newCapacity)
        val newExtras = getExtras()

        if (oldHead == -1) {
            head = -1
            tail = -1
            return
        }

        head = newIndices[oldHead]
        tail = newIndices[tail]

        var newPrevIndex = -1
        var newIndex = newIndices[oldHead]

        iterateChain(oldHead, oldNext) { oldIndex ->
            val oldNextIndex = oldNext[oldIndex]
            val newNextIndex = newIndices[oldNextIndex]

            next[newIndex] = newNextIndex
            prev[newIndex] = newPrevIndex

            newPrevIndex = newIndex
            newIndex = newNextIndex

            newExtras?.set(newIndex, oldExtras?.get(oldIndex))
        }
    }

    internal inline fun resizeStorage(newCapacity: Int, calculateNewIndex: (Int) -> Int) {
        val oldHead = head
        val oldNext = next
        val oldExtras = getExtras()

        initializeStorage(newCapacity)
        val newExtras = getExtras()

        iterateChain(oldHead, oldNext) { oldIndex ->
            val newIndex = calculateNewIndex(oldIndex)

            if (head == -1) {
                head = newIndex
                tail = newIndex
            }else {
                next[tail] = newIndex
                prev[newIndex] = tail
                tail = newIndex
            }

            newExtras?.set(newIndex, oldExtras?.get(oldIndex))
        }
    }

    protected open fun getExtras(): Array<Any?>? = null

    internal open fun addToEnd(index: Int) {
        if (head == -1) {
            head = index
            tail = index
            return
        }

        next[tail] = index
        prev[index] = tail
        tail = index
    }

    internal open fun moveToEnd(index: Int) {
        if (index == tail) {
            return
        }

        val prevIndex = prev[index]
        val nextIndex = next[index]

        if (index == head) {
            head = nextIndex
        }

        if (prevIndex != -1) {
            next[prevIndex] = nextIndex
        }

        if (nextIndex != -1) {
            prev[nextIndex] = prevIndex
        }

        next[tail] = index
        prev[index] = tail
        next[index] = -1
        tail = index
    }

    internal open fun remove(index: Int) {
        if (index == head) {
            head = next[index]
        }

        if (index == tail) {
            tail = prev[index]
        }

        val prevIndex = prev[index]
        val nextIndex = next[index]

        if (prevIndex != -1) {
            next[prevIndex] = nextIndex
        }

        if (nextIndex != -1) {
            prev[nextIndex] = prevIndex
        }

        next[index] = -1
        prev[index] = -1
    }

    internal open fun clear() {
        head = -1
        tail = -1
        next.fill(-1)
        prev.fill(-1)
    }
}

internal class MutableTimedChain(
    initialCapacity: Int,
    private val timeSource: TimeSource = TimeSource.Monotonic,
): MutableChain(initialCapacity) {

    @JvmField
    internal var timeMarks = Array<TimeMark?>(initialCapacity) { null }

    override fun initializeStorage(capacity: Int) {
        super.initializeStorage(capacity)
        timeMarks = Array(capacity) { null }
    }

    internal fun getTimeMark(index: Int): TimeMark? = timeMarks[index]

    @Suppress("UNCHECKED_CAST")
    override fun getExtras(): Array<Any?> = timeMarks as Array<Any?>

    override fun addToEnd(index: Int) {
        super.addToEnd(index)
        timeMarks[index] = timeSource.markNow()
    }

    override fun moveToEnd(index: Int) {
        super.moveToEnd(index)
        timeMarks[index] = timeSource.markNow()
    }

    override fun remove(index: Int) {
        super.remove(index)
        timeMarks[index] = null
    }

    override fun clear() {
        super.clear()
        timeMarks.fill(null)
    }
}

private inline fun iterateChain(
    head: Int,
    next: IntArray,
    action: (index: Int) -> Unit,
) {
    var index = head
    while (index != -1) {
        val nextIndex = next[index]
        action(index)
        index = nextIndex
    }
}
