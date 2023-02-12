package com.mayakapps.lrucache

import co.touchlab.stately.collections.IsoMutableMap

internal actual class ConcurrentMutableMap<K : Any, V : Any> actual constructor() : MutableMap<K, V>,
    IsoMutableMap<K, V>()