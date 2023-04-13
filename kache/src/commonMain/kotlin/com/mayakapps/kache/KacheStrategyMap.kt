package com.mayakapps.kache

internal fun <K : Any, V : Any> getMapByStrategy(strategy: KacheStrategy): MutableMap<K, V> = when (strategy) {
    KacheStrategy.LRU -> getLruMap()
    KacheStrategy.MRU -> AccessOrderedMap(ReverseLinkedHashMap(0, 0.75F))
    KacheStrategy.FIFO -> LinkedHashMap(0, 0.75F)
    KacheStrategy.FILO -> ReverseLinkedHashMap(0, 0.75F)
}

