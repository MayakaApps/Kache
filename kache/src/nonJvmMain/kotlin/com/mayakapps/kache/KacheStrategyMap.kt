package com.mayakapps.kache

actual fun <K : Any, V : Any> getMapByStrategy(strategy: KacheStrategy): MutableMap<K, V> = when (strategy) {
    KacheStrategy.LRU -> LinkedHashMap(0, 0.75F)
    KacheStrategy.MRU -> TODO()
    KacheStrategy.FIFO -> LinkedHashMap(0, 0.75F)
    KacheStrategy.FILO -> TODO()
}