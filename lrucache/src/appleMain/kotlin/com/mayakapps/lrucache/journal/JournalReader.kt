package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.Closeable

internal actual class JournalReader actual constructor(path: String) : Closeable {

    actual val isCorrupted: Boolean
        get() = TODO("Not yet implemented")

    actual fun readFully(): List<JournalOp> {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}