package com.mayakapps.kache.collections

@Suppress("FunctionName")
expect fun <K, V> LinkedHashMap(
    initialCapacity: Int = 0,
    loadFactor: Float = 0.75F,
    accessOrder: Boolean = false,
    reverseOrder: Boolean = false,
): MutableMap<K, V>