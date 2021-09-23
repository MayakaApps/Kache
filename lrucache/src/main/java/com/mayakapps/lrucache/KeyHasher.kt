package com.mayakapps.lrucache

import java.security.MessageDigest

class KeyHasher {
    private val hashedCache = LruCache<String, String>(maxSize = 1000)
    private val messageDigest = MessageDigest.getInstance("SHA-256")

    suspend fun hash(key: String): String {
        return hashedCache.getOrPut(key) {
            messageDigest.reset()
            messageDigest.update(key.encodeToByteArray())
            Utils.sha256BytesToHex(messageDigest.digest())
        }!! // Since our creation function never returns null. We can add !!
    }
}