package com.mayakapps.lrucache.io

import platform.Foundation.NSInputStream
import platform.Foundation.inputStreamWithFileAtPath

internal actual class FileInputStream actual constructor(path: String) :
    InputStream(base = NSInputStream.inputStreamWithFileAtPath(path) ?: throw FileNotFoundException(path)) {

    override fun read(): Int = defaultRead(this)
}