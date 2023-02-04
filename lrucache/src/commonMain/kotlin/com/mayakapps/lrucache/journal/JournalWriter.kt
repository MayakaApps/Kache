package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.BufferedOutputStream
import com.mayakapps.lrucache.io.Closeable
import com.mayakapps.lrucache.io.FileOutputStream
import com.mayakapps.lrucache.io.OutputStream

internal class JournalWriter(private val outputStream: OutputStream) : Closeable {

    constructor(journalFilename: String, append: Boolean = true) : this(
        BufferedOutputStream(FileOutputStream(journalFilename, append))
    )

    internal fun writeHeader() {
        outputStream.writeString(JOURNAL_MAGIC)
        outputStream.write(JOURNAL_VERSION)
        outputStream.flush()
    }

    internal fun writeDirty(key: String) = writeOperation(JournalOp.Dirty(key))
    internal fun writeClean(key: String) = writeOperation(JournalOp.Clean(key))
    internal fun writeRemove(key: String) = writeOperation(JournalOp.Remove(key))

    private fun writeOperation(operation: JournalOp) {
        outputStream.run {
            write(operation.opcode.toInt())
            writeLengthString(operation.key)
        }

        outputStream.flush()
    }

    override fun close() {
        outputStream.close()
    }
}