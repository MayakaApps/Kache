package com.mayakapps.kache

internal actual fun <K, V> getLruMap(): MutableMap<K, V> =
    LinkedHashMap(0, 0.75F, true)