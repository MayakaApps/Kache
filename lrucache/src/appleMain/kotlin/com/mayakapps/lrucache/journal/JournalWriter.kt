package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.Closeable

internal actual class JournalWriter actual constructor(path: String, append: Boolean) : Closeable {

    actual fun writeHeader() {
        TODO("Not yet implemented")
    }

    actual fun writeOperation(operation: JournalOp) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}