package com.mayakapps.kache.collection

import java.util.concurrent.ConcurrentHashMap

internal actual class ConcurrentMutableMap<K : Any, V : Any> actual constructor() : MutableMap<K, V>,
    ConcurrentHashMap<K, V>(0, 0.75F, 1) {

    override fun put(key: K, value: V): V? = super.put(key, value)

    override fun remove(key: K): V? = super<ConcurrentHashMap>.remove(key)
}
