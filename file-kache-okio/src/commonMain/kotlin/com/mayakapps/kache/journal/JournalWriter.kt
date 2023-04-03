package com.mayakapps.kache.journal

import okio.BufferedSink
import okio.Closeable

internal class JournalWriter(private val sink: BufferedSink) : Closeable {

    internal fun writeHeader() {
        sink.writeUtf8(JOURNAL_MAGIC)
        sink.writeByte(JOURNAL_VERSION.toInt())
        sink.flush()
    }

    internal fun writeAll(cleanKeys: Collection<String>, dirtyKeys: Collection<String>) {
        for (key in cleanKeys) writeEntry(JournalEntry.CLEAN, key)
        for (key in dirtyKeys) writeEntry(JournalEntry.DIRTY, key)
        sink.flush()
    }

    internal fun writeDirty(key: String) = writeEntryAndFlush(JournalEntry.DIRTY, key)

    internal fun writeClean(key: String) = writeEntryAndFlush(JournalEntry.CLEAN, key)

    internal fun writeRemove(key: String) = writeEntryAndFlush(JournalEntry.REMOVE, key)

    private fun writeEntryAndFlush(opcode: Byte, key: String) {
        writeEntry(opcode, key)
        sink.flush()
    }

    private fun writeEntry(opcode: Byte, key: String) {
        sink.writeByte(opcode.toInt())
        sink.writeByteLengthUtf8(key)
    }

    private fun BufferedSink.writeByteLengthUtf8(string: String) {
        writeByte(string.length)
        writeUtf8(string)
    }

    override fun close() {
        sink.close()
    }
}