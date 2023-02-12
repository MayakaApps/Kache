package com.mayakapps.lrucache.io

internal expect class ByteArrayOutputStream(): OutputStream {
    fun toByteArray(): ByteArray
}