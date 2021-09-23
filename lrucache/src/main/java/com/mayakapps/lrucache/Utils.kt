package com.mayakapps.lrucache

object Utils {
    // 32 bytes from sha-256 -> 64 hex chars.
    private val sha256Buffer = CharArray(64)

    // 20 bytes from sha-1 -> 40 chars.
    private val sha1Buffer = CharArray(40)

    /**
     * Returns the hex string of the given byte array representing a SHA256 hash.
     */
    fun sha256BytesToHex(bytes: ByteArray): String {
        return bytesToHex(bytes, sha256Buffer)
    }

    /**
     * Returns the hex string of the given byte array representing a SHA1 hash.
     */
    fun sha1BytesToHex(bytes: ByteArray): String {
        return bytesToHex(bytes, sha1Buffer)
    }

    private fun bytesToHex(bytes: ByteArray, hexChars: CharArray): String {
        var v: Int
        for (j in bytes.indices) {
            v = 0xFF and bytes[j].toInt()
            hexChars[j * 2] = HEX_CHAR_ARRAY[v ushr 4]
            hexChars[j * 2 + 1] = HEX_CHAR_ARRAY[v and 0x0F]
        }
        return String(hexChars)
    }

}

private val HEX_CHAR_ARRAY = "0123456789ABCDEF".toCharArray()