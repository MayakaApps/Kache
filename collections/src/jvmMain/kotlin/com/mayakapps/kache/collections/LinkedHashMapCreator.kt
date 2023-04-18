package com.mayakapps.kache.collections

actual fun <K, V> createLinkedHashMap(
    initialCapacity: Int,
    loadFactor: Float,
    accessOrder: Boolean,
    reverseOrder: Boolean
): MutableMap<K, V> = when {
    !accessOrder && !reverseOrder -> java.util.LinkedHashMap(initialCapacity, loadFactor, false)
    accessOrder && !reverseOrder -> java.util.LinkedHashMap(initialCapacity, loadFactor, true)
    !accessOrder && reverseOrder -> TODO()
    else -> TODO()
}