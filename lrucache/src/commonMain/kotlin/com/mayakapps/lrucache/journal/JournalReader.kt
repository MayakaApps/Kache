package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.Closeable
import com.mayakapps.lrucache.io.InputStream

internal class JournalReader(private val inputStream: InputStream) : Closeable {

    internal fun validateHeader() {
        val magic = try {
            inputStream.readString(JOURNAL_MAGIC.length)
        } catch (ex: JournalEOFException) {
            throw JournalInvalidHeaderException("File size is less than journal magic code size")
        }

        val version = inputStream.read()

        if (magic != JOURNAL_MAGIC) throw JournalInvalidHeaderException("Journal magic ($magic) doesn't match")
        if (version != JOURNAL_VERSION) throw JournalInvalidHeaderException("Journal version ($version) doesn't match")
    }

    internal fun readEntry(): JournalEntry? {
        val opcodeId = inputStream.read()
        if (opcodeId == -1) return null // Expected EOF

        val key = inputStream.readString()

        return JournalEntry(opcodeId, key)
    }

    override fun close() {
        inputStream.close()
    }

    // Read Helpers

    private fun InputStream.readString(): String {
        val length = read()
        return if (length != -1) readString(length)
        else throw JournalEOFException()
    }

    private fun InputStream.readString(length: Int) = readBytes(length).decodeToString()

    private fun InputStream.readBytes(count: Int) =
        ByteArray(count).also { if (read(it) != count) throw JournalEOFException() }
}