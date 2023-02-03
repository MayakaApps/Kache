package com.mayakapps.lrucache.io

import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.*

internal actual class ByteArrayOutputStream actual constructor() :
    OutputStream(base = NSOutputStream.outputStreamToMemory()) {

    override fun write(byte: Int): Unit = defaultWrite(this, byte)

    actual fun toByteArray(): ByteArray {
        val data = safeBase.propertyForKey(NSStreamDataWrittenToMemoryStreamKey) as NSData

        @OptIn(UnsafeNumber::class)
        val length = data.length.convert<UInt>().toInt()
        if (length == 0) return ByteArray(size = 0)

        return ByteArray(length).apply {
            usePinned { pinned ->
                data.getBytes(pinned.addressOf(0))
            }
        }
    }
}