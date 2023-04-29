package com.mayakapps.kache

/**
 * A snapshot of the keys in the cache.
 *
 * [keys] are the keys that are currently in the cache. While, [underCreationKeys] are the keys that are currently
 * under creation.
 */
data class KacheKeys<K>(val keys: Set<K>, val underCreationKeys: Set<K>)