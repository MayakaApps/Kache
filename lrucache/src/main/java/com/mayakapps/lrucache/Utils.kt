package com.mayakapps.lrucache

internal fun ByteArray.toHexString(): String =
    buildString(size * 2) {
        this@toHexString.forEach {
            val byte = 0xFF and it.toInt()
            append(HEX_CHAR_ARRAY[byte ushr 4])
            append(HEX_CHAR_ARRAY[byte and 0x0F])
        }
    }

@Suppress("SpellCheckingInspection")
private val HEX_CHAR_ARRAY = "0123456789ABCDEF".toCharArray()