package com.mayakapps.lrucache

internal fun ByteArray.toHexString(): String =
    buildString(size * 2) {
        this@toHexString.forEach {
            val byte = 0xFF and it.toInt()
            append(hexCharArray[byte ushr 4])
            append(hexCharArray[byte and 0x0F])
        }
    }

@Suppress("SpellCheckingInspection")
private val hexCharArray = "0123456789ABCDEF".toCharArray()