package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.Closeable
import com.mayakapps.lrucache.io.OutputStream

internal class JournalWriter(private val outputStream: OutputStream) : Closeable {

    internal fun writeHeader() {
        outputStream.writeString(JOURNAL_MAGIC)
        outputStream.write(JOURNAL_VERSION)
        outputStream.flush()
    }

    internal fun writeAll(cleanKeys: Collection<String>, dirtyKeys: Collection<String>) {
        for (key in cleanKeys) writeOperation(Opcode.CLEAN, key)
        for (key in dirtyKeys) writeOperation(Opcode.DIRTY, key)
        outputStream.flush()
    }

    internal fun writeDirty(key: String) = writeOperationAndFlush(Opcode.DIRTY, key)

    internal fun writeClean(key: String) = writeOperationAndFlush(Opcode.CLEAN, key)

    internal fun writeRemove(key: String) = writeOperationAndFlush(Opcode.REMOVE, key)

    private fun writeOperationAndFlush(opcode: Opcode, key: String) {
        writeOperation(opcode, key)
        outputStream.flush()
    }

    private fun writeOperation(opcode: Opcode, key: String) {
        outputStream.write(opcode.id)
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