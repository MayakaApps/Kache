package com.mayakapps.lrucache.io

import platform.Foundation.NSOutputStream
import platform.Foundation.outputStreamToFileAtPath

internal actual class FileOutputStream actual constructor(path: String, append: Boolean = true) :
    OutputStream(base = NSOutputStream.outputStreamToFileAtPath(path, append).apply { open() }) {

    override fun write(byte: Int) = defaultWrite(this, byte)
}