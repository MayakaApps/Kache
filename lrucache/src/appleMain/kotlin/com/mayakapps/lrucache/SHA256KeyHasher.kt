package com.mayakapps.lrucache

actual object SHA256KeyHasher : KeyTransformer {
    private val hashedCache = LruCache<String, String>(maxSize = 1000)

    actual override suspend fun transform(oldKey: String): String = hashedCache.getOrPut(oldKey) {
        TODO()
    }!! // Since our creation function never returns null, we can use not-null assertion
}