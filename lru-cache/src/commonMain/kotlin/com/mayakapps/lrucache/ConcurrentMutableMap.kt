package com.mayakapps.lrucache

internal expect class ConcurrentMutableMap<K : Any, V : Any>() : MutableMap<K, V>