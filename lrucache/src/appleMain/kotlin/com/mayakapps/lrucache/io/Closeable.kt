package com.mayakapps.lrucache.io

internal actual interface Closeable {
    actual fun close()
}

internal actual inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
    return try {
        block(this)
    } finally {
        this?.close()
    }
}