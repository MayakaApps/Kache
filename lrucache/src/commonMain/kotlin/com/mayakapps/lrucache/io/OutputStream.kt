package com.mayakapps.lrucache.io

internal expect abstract class OutputStream : Closeable {

    abstract fun write(byte: Int)

    fun write(buffer: ByteArray)

    fun write(buffer: ByteArray, offset: Int, length: Int)

    fun flush()
}