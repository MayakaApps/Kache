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

import androidx.collection.*
import androidx.collection.DefaultScatterCapacity
import androidx.collection.MutableScatterMap
import androidx.collection.hash
import androidx.collection.isFull
import kotlin.jvm.JvmField

internal class MutableChainedScatterMap<K, V>(
    initialCapacity: Int = DefaultScatterCapacity,
    @JvmField internal val accessChain: MutableChain? = null,
    @JvmField internal val insertionChain: MutableChain? = null,
    @JvmField internal val accessOrder: Boolean = true,
) : MutableScatterMap<K, V>(initialCapacity) {

    init {
        require(accessChain != null || insertionChain != null) { "At least, one chain must be not null" }
        accessChain?.initializeStorage(_capacity)
        insertionChain?.initializeStorage(_capacity)
    }

    @JvmField
    internal val mainChain: MutableChain = if (accessOrder) {
        accessChain ?: insertionChain!!
    } else {
        insertionChain ?: accessChain!!
    }

    override fun afterAccess(index: Int) {
        accessChain?.moveToEnd(index)
    }

    override fun afterInsertion(index: Int) {
        accessChain?.addToEnd(index)
        insertionChain?.addToEnd(index)
    }

    override fun afterReplacement(index: Int) {
        accessChain?.moveToEnd(index)
        insertionChain?.moveToEnd(index)
    }

    override fun afterRemoval(index: Int) {
        accessChain?.remove(index)
        insertionChain?.remove(index)
    }

    override fun clear() {
        super.clear()
        accessChain?.clear()
        insertionChain?.clear()
    }

    override fun resizeStorage(newCapacity: Int) {
        val previousMetadata = metadata
        val previousKeys = keys
        val previousValues = values
        val previousMainChain = mainChain.shallowCopy()
        val previousAccessoryChain = (if (accessOrder) insertionChain else accessChain)?.shallowCopy()

        initializeStorage(newCapacity)
        accessChain?.initializeStorage(_capacity)
        insertionChain?.initializeStorage(_capacity)

        val newKeys = keys
        val newValues = values
        val newIndices = IntArray(capacity)
        val newMainChain = mainChain
        val newAccessoryChain = if (accessOrder) insertionChain else accessChain

        previousMainChain.forEachIndexed { i ->
            if (isFull(previousMetadata, i)) {
                val previousKey = previousKeys[i]
                val hash = hash(previousKey)
                val index = findFirstAvailableSlot(h1(hash))

                writeMetadata(index, h2(hash).toLong())
                newKeys[index] = previousKey
                newValues[index] = previousValues[i]
                newMainChain.addToEnd(index)
                newIndices[i] = index
            }
        }

        previousAccessoryChain?.forEachIndexed { i ->
            newAccessoryChain?.addToEnd(newIndices[i])
        }
    }

    fun getKeySet(accessOrder: Boolean = this.accessOrder, reversed: Boolean = false): Set<K> = object : Set<K> {
        override val size: Int get() = this@MutableChainedScatterMap._size

        override fun isEmpty(): Boolean = this@MutableChainedScatterMap.isEmpty()

        override fun iterator(): Iterator<K> = object : Iterator<K> {
            private val chain: MutableChain = if (accessOrder) {
                accessChain ?: insertionChain!!
            } else {
                insertionChain ?: accessChain!!
            }

            private var nextIndex = if (reversed) {
                chain.tail
            } else {
                chain.head
            }

            override fun hasNext(): Boolean = nextIndex != -1

            override fun next(): K {
                val index = nextIndex
                nextIndex = if (reversed) {
                    chain.prev[nextIndex]
                } else {
                    chain.next[nextIndex]
                }
                @Suppress("UNCHECKED_CAST")
                return values[index] as K
            }
        }

        override fun containsAll(elements: Collection<K>): Boolean =
            elements.all { this@MutableChainedScatterMap.containsKey(it) }

        override fun contains(element: K): Boolean = this@MutableChainedScatterMap.containsKey(element)
    }

    /**
     * Iterates over every key/value pair stored in this map by invoking the [block] function. The
     * order of iteration is defined by the [accessOrder] parameter. The order of iteration can be
     * reversed by setting the [reversed] parameter to `true`.
     */
    inline fun forEach(
        accessOrder: Boolean = this.accessOrder,
        reversed: Boolean = false,
        block: (key: K, value: V) -> Unit
    ) {
        val chain: MutableChain = if (accessOrder) {
            accessChain ?: insertionChain!!
        } else {
            insertionChain ?: accessChain!!
        }

        chain.forEachIndexed(reversed = reversed) { index ->
            @Suppress("UNCHECKED_CAST")
            block(keys[index] as K, values[index] as V)
        }
    }

    /**
     * Removes every key/value pair stored in this map, invoking the [callback] function for each
     * pair. The order of iteration is defined by the [accessOrder] parameter. The order of
     * iteration can be reversed by setting the [reversed] parameter to `true`. The removal of
     * key/value pairs can be stopped by returning `true` from the [callback] function.
     */
    inline fun removeAllWithCallback(
        reversed: Boolean = false,
        accessOrder: Boolean = this.accessOrder,
        callback: (key: K, value: V) -> Boolean,
    ) {
        val chain: MutableChain = if (accessOrder) {
            accessChain ?: insertionChain!!
        } else {
            insertionChain ?: accessChain!!
        }

        chain.forEachIndexed(reversed = reversed) { index ->
            val key = keys[index]
            val value = values[index]
            removeValueAt(index)
            @Suppress("UNCHECKED_CAST")
            if (callback(key as K, value as V)) {
                return
            }
        }
    }
}
