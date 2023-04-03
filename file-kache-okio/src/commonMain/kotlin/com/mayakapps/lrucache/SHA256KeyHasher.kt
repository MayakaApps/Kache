package com.mayakapps.lrucache

import okio.ByteString.Companion.encodeUtf8

/**
 * An object that implements [KeyTransformer] and transforms keys to an SHA-256 hash of them.
 *
 * The last 1000 hashed values are cached in memory. This is used as the default [KeyTransformer] for [DiskLruCache].
 */
object SHA256KeyHasher : KeyTransformer {
    private val hashedCache = LruCache<String, String>(maxSize = 1000)

    /**
     * Returns an SHA-256 hash of [oldKey] which may be newly generated or previously cached.
     */
    override suspend fun transform(oldKey: String): String = hashedCache.getOrPut(oldKey) {
        oldKey.encodeUtf8().sha256().hex().uppercase()
    }!! // Since our creation function never returns null, we can use not-null assertion
}