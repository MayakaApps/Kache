package com.mayakapps.lrucache

import com.mayakapps.lrucache.journal.JOURNAL_MAGIC
import com.mayakapps.lrucache.journal.JOURNAL_VERSION
import com.mayakapps.lrucache.journal.JournalOp
import java.io.*

// This is more than enough as the buffer is flushing after each operation
private const val BUFFER_SIZE = 256

internal class JournalWriter(journalFile: File, append: Boolean = true) : Closeable {
    private val outputStream =
        DataOutputStream(FileOutputStream(journalFile, append).buffered(BUFFER_SIZE))

    internal fun writeHeader() {
        outputStream.writeString(JOURNAL_MAGIC)
        outputStream.writeByte(JOURNAL_VERSION.toInt())
        outputStream.flush()
    }

    internal fun writeDirty(key: String) = writeOperation(JournalOp.Dirty(key))
    internal fun writeClean(key: String) = writeOperation(JournalOp.Clean(key))
    internal fun writeRemove(key: String) = writeOperation(JournalOp.Remove(key))

    private fun writeOperation(operation: JournalOp) {
        outputStream.run {
            writeByte(operation.opcode.toInt())
            writeLengthString(operation.key)
        }

        outputStream.flush()
    }

    override fun close() {
        outputStream.close()
    }
}


internal class JournalReader(journalFile: File) : Closeable {
    private val inputStream = DataInputStream(journalFile.inputStream())

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
        isCorrupted = true
        null
    } catch (ex: IllegalArgumentException) {
        isCorrupted = true
        null
    }

    override fun close() {
        inputStream.close()
    }
}