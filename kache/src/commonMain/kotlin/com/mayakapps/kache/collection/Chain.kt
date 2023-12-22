package com.mayakapps.kache.collection

import kotlin.jvm.JvmField

internal class Chain(capacity: Int) {
    @JvmField
    var head = -1

    @JvmField
    var tail = -1

    @JvmField
    val next = IntArray(capacity) { -1 }

    @JvmField
    val prev = IntArray(capacity) { -1 }

    fun addToEnd(index: Int) {
        if (head == -1) {
            head = index
            tail = index
            return
        }

        next[tail] = index
        prev[index] = tail
        tail = index
    }

    fun moveToEnd(index: Int) {
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

    fun remove(index: Int) {
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

    fun clear() {
        head = -1
        tail = -1
        next.fill(-1)
        prev.fill(-1)
    }

    inline fun forEachIndexed(action: (index: Int) -> Unit) {
        var index = head
        while (index != -1) {
            val nextIndex = next[index]
            action(index)
            index = nextIndex
        }
    }

    inline fun reversedForEachIndexed(action: (index: Int) -> Unit) {
        var index = tail
        while (index != -1) {
            val prevIndex = prev[index]
            action(index)
            index = prevIndex
        }
    }
}
