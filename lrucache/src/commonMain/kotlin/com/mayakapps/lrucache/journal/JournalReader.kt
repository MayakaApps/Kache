package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.Closeable

internal expect class JournalReader(path: String) : Closeable {
    val isCorrupted: Boolean

    fun readFully(): List<JournalOp>
}