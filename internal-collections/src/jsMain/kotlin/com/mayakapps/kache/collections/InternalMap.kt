/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * A copy of InternalMap.kt from Kotlin 1.8.20 from:
 *   https://github.com/JetBrains/kotlin/blob/v1.8.20/libraries/stdlib/js/src/kotlin/collections/InternalMap.kt
 * Changes are marked with "MODIFICATION" comments.
 */

// MODIFICATION: package renamed to com.mayakapps.kache.collections
package com.mayakapps.kache.collections

/**
 * The common interface of [InternalStringMap] and [InternalHashCodeMap].
 */
internal interface InternalMap<K, V> : MutableIterable<MutableMap.MutableEntry<K, V>> {
    val equality: EqualityComparator
    val size: Int
    operator fun contains(key: K): Boolean
    operator fun get(key: K): V?

    fun put(key: K, value: V): V?
    fun remove(key: K): V?
    fun clear(): Unit

    fun createJsMap(): dynamic {
        val result = js("Object.create(null)")
        // force to switch object representation to dictionary mode
        result["foo"] = 1
        jsDeleteProperty(result, "foo")
        return result
    }
}