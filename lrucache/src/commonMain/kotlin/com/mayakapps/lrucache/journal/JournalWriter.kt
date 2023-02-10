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
        for (key in cleanKeys) writeEntry(JournalEntry.CLEAN, key)
        for (key in dirtyKeys) writeEntry(JournalEntry.DIRTY, key)
        outputStream.flush()
    }

    internal fun writeDirty(key: String) = writeEntryAndFlush(JournalEntry.DIRTY, key)

    internal fun writeClean(key: String) = writeEntryAndFlush(JournalEntry.CLEAN, key)

    internal fun writeRemove(key: String) = writeEntryAndFlush(JournalEntry.REMOVE, key)

    private fun writeEntryAndFlush(opcode: Int, key: String) {
        writeEntry(opcode, key)
        outputStream.flush()
    }

    private fun writeEntry(opcode: Int, key: String) {
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