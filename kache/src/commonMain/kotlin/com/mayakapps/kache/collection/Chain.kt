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

import androidx.collection.internal.EMPTY_INTS
import kotlin.jvm.JvmField

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
            var index = tail
            while (index != -1) {
                val prevIndex = prev[index]
                action(index)
                index = prevIndex
            }
        } else {
            var index = head
            while (index != -1) {
                val nextIndex = next[index]
                action(index)
                index = nextIndex
            }
        }
    }

    internal fun shallowCopy(): Chain {
        val copy = MutableChain(0)
        copy.head = head
        copy.tail = tail
        copy.next = next
        copy.prev = prev
        return copy
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

internal class MutableChain(initialCapacity: Int) : Chain() {

    init {
        require(initialCapacity >= 0) { "Capacity must be a positive value." }
        if (initialCapacity > 0) {
            initializeStorage(initialCapacity)
        }
    }

    internal fun initializeStorage(capacity: Int) {
        head = -1
        tail = -1
        next = IntArray(capacity) { -1 }
        prev = IntArray(capacity) { -1 }
    }

    internal fun addToEnd(index: Int) {
        if (head == -1) {
            head = index
            tail = index
            return
        }

        next[tail] = index
        prev[index] = tail
        tail = index
    }

    internal fun moveToEnd(index: Int) {
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

    internal fun remove(index: Int) {
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

    internal fun clear() {
        head = -1
        tail = -1
        next.fill(-1)
        prev.fill(-1)
    }
}
