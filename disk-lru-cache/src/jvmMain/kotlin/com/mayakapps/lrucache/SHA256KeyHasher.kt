package com.mayakapps.lrucache

import java.security.MessageDigest

actual object SHA256KeyHasher : KeyTransformer {
    private val hashedCache = LruCache<String, String>(maxSize = 1000)
    private val messageDigest = MessageDigest.getInstance("SHA-256")

    actual override suspend fun transform(oldKey: String): String = hashedCache.getOrPut(oldKey) {
        messageDigest.run {
            reset()
            update(oldKey.encodeToByteArray())
            digest().toHexString()
        }
    }!! // Since our creation function never returns null, we can use not-null assertion
}