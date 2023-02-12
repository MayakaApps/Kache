package com.mayakapps.lrucache.io

import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import platform.Foundation.NSInputStream
import platform.Foundation.inputStreamWithFileAtPath

internal actual class FileInputStream actual constructor(path: String) :
    InputStream(
        base = (NSInputStream.inputStreamWithFileAtPath(path) ?: throw FileNotFoundException(path)).apply {
            open()
            streamError?.let { error ->
                @OptIn(UnsafeNumber::class)
                throw FileNotFoundException("$path: code: ${error.code.convert<Int>()} - description: ${error.localizedDescription}")
            }
        }
    ) {

    override fun read(): Int = defaultRead(this)
}