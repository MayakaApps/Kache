package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.IOException
import com.mayakapps.lrucache.io.InputStream
import com.mayakapps.lrucache.io.OutputStream

internal const val JOURNAL_MAGIC = "JOURNAL"
internal const val JOURNAL_VERSION = 1

internal fun OutputStream.writeLengthString(string: String) {
    write(string.length)
    writeString(string)
}

internal fun OutputStream.writeString(string: String) = write(string.encodeToByteArray())

internal fun InputStream.readString(): String? {
    val length = read()
    return if (length == -1) null
    else readString(length)
}

internal fun InputStream.readString(length: Int) = readBytes(length).decodeToString()
internal fun InputStream.readBytes(count: Int) = ByteArray(count).also { if (read(it) != count) throw IOException() }