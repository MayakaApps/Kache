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

    internal fun writeAll(cleanKeys: Collection<String>, dirtyKeys: Collection<String>) {
        for (key in cleanKeys) writeOperation(OPCODE_CLEAN, key)
        for (key in dirtyKeys) writeOperation(OPCODE_DIRTY, key)
        outputStream.flush()
    }

    internal fun writeDirty(key: String) = writeOperationAndFlush(OPCODE_DIRTY, key)

    internal fun writeClean(key: String) = writeOperationAndFlush(OPCODE_CLEAN, key)

    internal fun writeRemove(key: String) = writeOperationAndFlush(OPCODE_REMOVE, key)

    private fun writeOperationAndFlush(opcode: Int, key: String) {
        writeOperation(opcode, key)
        outputStream.flush()
    }

    private fun writeOperation(opcode: Int, key: String) {
        outputStream.write(opcode)
        outputStream.writeLengthString(key)
    }

    override fun close() {
        outputStream.close()
    }

    // Write Helpers

    private fun OutputStream.writeLengthString(string: String) {
        write(string.length)
        writeString(string)
    }

    private fun OutputStream.writeString(string: String) = write(string.encodeToByteArray())
}