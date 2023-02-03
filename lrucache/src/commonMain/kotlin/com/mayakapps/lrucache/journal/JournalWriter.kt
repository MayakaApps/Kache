package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.Closeable

internal expect class JournalWriter(path: String, append: Boolean = true) : Closeable {
    fun writeHeader()

    fun writeOperation(operation: JournalOp)
}

internal fun JournalWriter.writeDirty(key: String) = writeOperation(JournalOp.Dirty(key))

internal fun JournalWriter.writeClean(key: String) = writeOperation(JournalOp.Clean(key))

internal fun JournalWriter.writeRemove(key: String) = writeOperation(JournalOp.Remove(key))