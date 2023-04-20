package com.mayakapps.kache.collections

expect fun <K, V> createLinkedHashMap(
    initialCapacity: Int = 0,
    loadFactor: Float = 0.75F,
    accessOrder: Boolean = false,
    reverseOrder: Boolean = false,
): MutableMap<K, V>