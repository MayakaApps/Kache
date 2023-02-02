package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.Closeable
import com.mayakapps.lrucache.readString
import java.io.DataInputStream
import java.io.File
import java.io.IOException

internal actual class JournalReader(journalFile: File) : Closeable {

    actual constructor(path: String) : this(File(path))

    private val inputStream = DataInputStream(journalFile.inputStream())

    private var internalIsCorrupted = false

    actual val isCorrupted get() = internalIsCorrupted

    actual fun readFully(): List<JournalOp> {
        val result = mutableListOf<JournalOp>()

        try {
            validateHeader()
        } catch (ex: IllegalStateException) {
            internalIsCorrupted = true
            return emptyList()
        }

        while (inputStream.available() > 0) {
            val operation = readOperation()
            if (operation != null) result += operation else break
        }

        return result
    }

    private fun validateHeader() {
        val magic = inputStream.readString(JOURNAL_MAGIC.length)
        val version = inputStream.readByte()

        check(magic == JOURNAL_MAGIC) { "Journal magic string doesn't match" }
        check(version == JOURNAL_VERSION) { "Journal version doesn't match" }
    }

    private fun readOperation(): JournalOp? = try {
        JournalOp.create(
            opcode = inputStream.readByte(),
            key = inputStream.readString(),
        )
    } catch (ex: IOException) {
        internalIsCorrupted = true
        null
    } catch (ex: IllegalArgumentException) {
        internalIsCorrupted = true
        null
    }

    override fun close() {
        inputStream.close()
    }
}