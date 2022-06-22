package com.mayakapps.lrucache

import java.security.MessageDigest

/**
 * Base interface that can be used to implement custom key transformers for [DiskLruCache].
 *
 * In most cases, you don't need to implement one yourself. You can use [SHA256KeyHasher] instead or `null` if your
 * keys are already safe for filenames.
 */
fun interface KeyTransformer {

    /**
     * Returns a new transformed version of [oldKey]. Please note that the new key must follow filename guidelines.
     * To be safe, just limit yourself to characters, numbers, and underscores when possible. For more information,
     * see [this Wikipedia page](https://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations).
     */
    suspend fun transform(oldKey: String): String
}

/**
 * An object that implements [KeyTransformer] and transforms keys to an SHA-256 hash of them.
 *
 * The last 1000 hashed values are cached in memory. This is used as the default [KeyTransformer] for [DiskLruCache].
 */
object SHA256KeyHasher : KeyTransformer {
    private val hashedCache = LruCache<String, String>(maxSize = 1000)
    private val messageDigest = MessageDigest.getInstance("SHA-256")

    /**
     * Returns an SHA-256 hash of [oldKey] which may be newly generated or previously cached.
     */
    override suspend fun transform(oldKey: String): String = hashedCache.getOrPut(oldKey) {
        messageDigest.run {
            reset()
            update(oldKey.encodeToByteArray())
            digest().toHexString()
        }
    }!! // Since our creation function never returns null, we can use not-null assertion
}