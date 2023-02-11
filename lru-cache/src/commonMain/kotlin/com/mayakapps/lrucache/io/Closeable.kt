package com.mayakapps.lrucache.io

internal expect interface Closeable {
    fun close()
}

internal expect inline fun <T : Closeable?, R> T.use(block: (T) -> R): R