package com.mayakapps.lrucache.io

import kotlinx.cinterop.*
import platform.Foundation.NSOutputStream

internal actual abstract class OutputStream(private val base: NSOutputStream? = null) : Closeable {

    actual abstract fun write(byte: Int)

    actual open fun write(buffer: ByteArray) = write(buffer, 0, buffer.size)

    actual open fun write(buffer: ByteArray, offset: Int, length: Int) {
        check(offset > 0 && offset + length <= buffer.size)
        if (buffer.isEmpty()) return

        @OptIn(UnsafeNumber::class)
        return buffer.usePinned { pinnedBuffer ->
            safeBase.write(pinnedBuffer.addressOf(offset).reinterpret(), length.convert())
        }
    }

    override fun close() {
        safeBase.close()
    }

    actual open fun flush() {}

    protected val safeBase get() = base ?: throw IllegalStateException("SimpleInputStream has no base")

    companion object {
        fun defaultWrite(outputStream: OutputStream, byte: Int) =
            byteArrayOf(byte.toByte()).let { outputStream.write(it) }
    }
}