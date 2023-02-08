package com.mayakapps.lrucache.io

import kotlinx.cinterop.*
import platform.Foundation.NSInputStream

internal actual abstract class InputStream(private val base: NSInputStream? = null) : Closeable {

    actual constructor() : this(null)

    actual abstract fun read(): Int

    actual open fun read(buffer: ByteArray): Int = read(buffer, 0, buffer.size)

    actual open fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        require(offset >= 0 && offset + length <= buffer.size)
        if (buffer.isEmpty()) return 0

        @OptIn(UnsafeNumber::class)
        return buffer.usePinned { pinnedBuffer ->
            val read: Int = safeBase.read(pinnedBuffer.addressOf(offset).reinterpret(), length.convert()).convert()
            when {
                read > 0 -> read
                read == 0 -> -1
                else -> {
                    val baseError = safeBase.streamError
                    if (baseError == null) throw IOException()
                    else throw IOException(baseError.localizedDescription)
                }
            }
        }
    }

    override fun close() {
        safeBase.close()
    }

    private val safeBase get() = base ?: throw IllegalStateException("InputStream has no base")

    companion object {
        fun defaultRead(inputStream: InputStream): Int {
            val buffer = ByteArray(1)
            return if (inputStream.read(buffer) <= 0) -1
            else buffer[0].toInt()
        }
    }
}