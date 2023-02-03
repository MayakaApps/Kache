package com.mayakapps.lrucache.io

internal expect abstract class InputStream : Closeable {

    abstract fun read(): Int

    fun read(buffer: ByteArray): Int

    fun read(buffer: ByteArray, offset: Int, length: Int): Int
}