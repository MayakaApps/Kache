package com.mayakapps.lrucache.io

import kotlinx.cinterop.*
import platform.Foundation.NSInputStream

internal actual abstract class InputStream(private val base: NSInputStream? = null) : Closeable {

    actual abstract fun read(): Int

    actual open fun read(buffer: ByteArray): Int = read(buffer, 0, buffer.size)

    actual open fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        check(offset > 0 && offset + length <= buffer.size)
        if (buffer.isEmpty()) return 0

        @OptIn(UnsafeNumber::class)
        return buffer.usePinned { pinnedBuffer ->
            safeBase.read(pinnedBuffer.addressOf(offset).reinterpret(), length.convert())
        }
    }

    override fun close() {
        safeBase.close()
    }

    private val safeBase get() = base ?: throw IllegalStateException("SimpleInputStream has no base")

    companion object {
        fun defaultRead(inputStream: InputStream) =
            ByteArray(1).also { inputStream.read(it) }[0].toInt()
    }
}