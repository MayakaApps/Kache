package com.mayakapps.lrucache

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

internal fun File.renameToOrThrow(dest: File, deleteDest: Boolean) {
    if (deleteDest) dest.deleteOrThrow()
    if (!renameTo(dest)) throw IOException()
}

internal fun File.deleteOrThrow() {
    if (exists() && !delete()) throw IOException()
}


internal fun DataOutputStream.writeLengthString(string: String) {
    writeByte(string.length)
    writeString(string)
}

internal fun DataOutputStream.writeString(string: String) = write(string.encodeToByteArray())

internal fun DataInputStream.readString() = readString(readByte().toInt())
internal fun DataInputStream.readString(length: Int) = readBytes(length).decodeToString()
internal fun DataInputStream.readBytes(count: Int) = ByteArray(count).also { read(it) }


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