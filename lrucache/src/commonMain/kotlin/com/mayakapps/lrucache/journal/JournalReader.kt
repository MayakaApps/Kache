package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.*

internal class JournalReader(private val inputStream: InputStream) : Closeable {

    constructor(journalFilename: String) : this(BufferedInputStream(FileInputStream(journalFilename)))

    var isCorrupted = false
        private set

    fun readFully(): List<JournalOp> {
        val result = mutableListOf<JournalOp>()

        try {
            validateHeader()
        } catch (ex: IllegalStateException) {
            isCorrupted = true
            return emptyList()
        }

        while (true) {
            val operation = readOperation()
            if (operation != null) result += operation else break
        }

        return result
    }

    private fun validateHeader() {
        val magic = inputStream.readString(JOURNAL_MAGIC.length)
        val version = inputStream.read()

        check(magic == JOURNAL_MAGIC) { "Journal magic string doesn't match" }
        check(version == JOURNAL_VERSION) { "Journal version doesn't match" }
    }

    private fun readOperation(): JournalOp? {
        return try {
            JournalOp.create(
                opcode = inputStream.read().toByte(),
                key = inputStream.readString() ?: return null,
            )
        } catch (ex: IOException) {
            isCorrupted = true
            null
        } catch (ex: IllegalArgumentException) {
            isCorrupted = true
            null
        }
    }

    override fun close() {
        inputStream.close()
    }
}