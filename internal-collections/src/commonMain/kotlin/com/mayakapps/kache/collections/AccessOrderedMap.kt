package com.mayakapps.kache.collections

internal class AccessOrderedMap<K, V>(
    private val baseMap: MutableMap<K, V>,
) : MutableMap<K, V> by baseMap {

    /**
     * Works similarly to Java LinkedHashMap with LRU order enabled.
     * Removes the existing item from the map if there is one, and then adds it back,
     * so the item is moved to the end.
     */
    override operator fun get(key: K): V? {
        val item = baseMap.remove(key)
        if (item != null) {
            baseMap[key] = item
        }

        return item
    }

    /**
     * Works similarly to Java LinkedHashMap with LRU order enabled.
     * Removes the existing item from the map if there is one,
     * then inserts the new item to the map, so the item is moved to the end.
     */
    override fun put(key: K, value: V): V? {
        val item = baseMap.remove(key)
        baseMap[key] = value

        return item
    }

    override fun putAll(from: Map<out K, V>) {
        for ((key, value) in from) {
            put(key, value)
        }
    }
}