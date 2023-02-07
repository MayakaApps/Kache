package com.mayakapps.lrucache.io

import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSInputStream
import platform.Foundation.dataWithBytes
import platform.Foundation.inputStreamWithData

internal actual class ByteArrayInputStream actual constructor(buffer: ByteArray) :
    InputStream(base = inputStreamWithByteArray(buffer)) {

    override fun read(): Int = defaultRead(this)

    companion object {
        private fun inputStreamWithByteArray(byteArray: ByteArray): NSInputStream {
            val data = if (byteArray.isEmpty()) NSData() else {
                byteArray.usePinned { pinned ->
                    @OptIn(UnsafeNumber::class)
                    NSData.dataWithBytes(pinned.addressOf(0), byteArray.size.convert())
                }
            }

            return NSInputStream.inputStreamWithData(data)!!
        }
    }
}