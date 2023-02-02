package com.mayakapps.lrucache

import kotlinx.cinterop.*
import platform.CoreCrypto.CC_LONG
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

actual object SHA256KeyHasher : KeyTransformer {
    private val hashedCache = LruCache<String, String>(maxSize = 1000)

    actual override suspend fun transform(oldKey: String): String = hashedCache.getOrPut(oldKey) {
        if (oldKey.isEmpty()) transform(null, 0.convert())
        else {
            val data = oldKey.encodeToByteArray()
            data.usePinned { dataPinned ->
                transform(dataPinned.addressOf(0), data.size.convert())
            }
        }
    }!! // Since our creation function never returns null, we can use not-null assertion

    private fun transform(data: CPointer<ByteVar>?, length: CC_LONG): String {
        val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
        digest.usePinned { digestPinned ->
            CC_SHA256(data, length, digestPinned.addressOf(0).reinterpret())
        }

        return digest.toHexString()
    }
}