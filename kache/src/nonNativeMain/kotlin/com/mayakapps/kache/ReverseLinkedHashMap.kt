package com.mayakapps.kache

internal actual class ReverseLinkedHashMap<K, V> : MutableMap<K, V> {

    actual constructor() {
        TODO("Not yet implemented")
    }

    actual constructor(initialCapacity: Int) {
        TODO("Not yet implemented")
    }

    actual constructor(initialCapacity: Int, loadFactor: Float) {
        TODO("Not yet implemented")
    }

    actual constructor(original: Map<out K, V>) {
        TODO("Not yet implemented")
    }

    actual override val size: Int
        get() = TODO("Not yet implemented")

    actual override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    actual override fun containsKey(key: K): Boolean {
        TODO("Not yet implemented")
    }

    actual override fun containsValue(value: @UnsafeVariance V): Boolean {
        TODO("Not yet implemented")
    }

    actual override fun get(key: K): V? {
        TODO("Not yet implemented")
    }

    actual override fun put(key: K, value: V): V? {
        TODO("Not yet implemented")
    }

    actual override fun remove(key: K): V? {
        TODO("Not yet implemented")
    }

    actual override fun putAll(from: Map<out K, V>) {
    }

    actual override fun clear() {
    }

    actual override val keys: MutableSet<K>
        get() = TODO("Not yet implemented")
    actual override val values: MutableCollection<V>
        get() = TODO("Not yet implemented")
    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = TODO("Not yet implemented")

}