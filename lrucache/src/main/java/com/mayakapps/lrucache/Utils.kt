package com.mayakapps.lrucache

import java.io.File
import java.io.IOException

internal fun File.renameToOrThrow(dest: File, deleteDest: Boolean) {
    if (deleteDest) dest.deleteOrThrow()
    if (!renameTo(dest)) throw IOException()
}

internal fun File.deleteOrThrow() {
    if (exists() && !delete()) throw IOException()
}

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