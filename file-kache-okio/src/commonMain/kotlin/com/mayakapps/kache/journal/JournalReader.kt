package com.mayakapps.kache.journal

import okio.BufferedSource
import okio.Closeable
import okio.EOFException

internal class JournalReader(private val source: BufferedSource) : Closeable {

    internal fun validateHeader() {
        val magic = try {
            source.readUtf8(JOURNAL_MAGIC.length.toLong())
        } catch (ex: EOFException) {
            throw JournalInvalidHeaderException("File size is less than journal magic code size")
        }

        val version = source.readByte()

        if (magic != JOURNAL_MAGIC) throw JournalInvalidHeaderException("Journal magic ($magic) doesn't match")
        if (version != JOURNAL_VERSION) throw JournalInvalidHeaderException("Journal version ($version) doesn't match")
    }

    internal fun readEntry(): JournalEntry? {
        val opcodeId = source.readByte()

        if (opcodeId == JournalEntry.EOJ) return null

        val key = source.readByteLengthUtf8()

        return JournalEntry(opcodeId, key)
    }

    override fun close() {
        source.close()
    }

    private fun BufferedSource.readByteLengthUtf8(): String {
        val length = readByte()
        return readUtf8(length.toLong())
    }
}