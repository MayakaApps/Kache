package com.mayakapps.kache

import com.mayakapps.kache.collections.createLinkedHashMap

enum class KacheStrategy {
    LRU, MRU, FIFO, FILO;

    internal fun <K : Any, V : Any> createMap(): MutableMap<K, V> =
        createLinkedHashMap(
            accessOrder = this == LRU || this == MRU,
            reverseOrder = this == MRU || this == FILO,
        )
}