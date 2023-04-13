package com.mayakapps.kache

internal expect fun <K, V> getLruMap(): MutableMap<K, V>