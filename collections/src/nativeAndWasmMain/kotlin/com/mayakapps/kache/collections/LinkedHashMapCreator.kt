package com.mayakapps.kache.collections

actual fun <K, V> createLinkedHashMap(
    initialCapacity: Int,
    loadFactor: Float,
    accessOrder: Boolean,
    reverseOrder: Boolean
): MutableMap<K, V> = when {
    !accessOrder && !reverseOrder -> kotlin.collections.LinkedHashMap(initialCapacity, loadFactor)
    accessOrder && !reverseOrder -> TODO()
    !accessOrder && reverseOrder -> TODO()
    else -> TODO()
}