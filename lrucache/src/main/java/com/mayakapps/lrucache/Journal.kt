package com.mayakapps.lrucache

import java.io.*

class JournalWriter(journalFile: File) : Closeable {
    private val outputStream = DataOutputStream(journalFile.outputStream().buffered(BUFFER_SIZE))

    fun writeFully(operations: List<JournalOp>) {
        writeHeader()
        operations.forEach { writeOperation(it) }
    }

    fun writeHeader() {
        outputStream.writeString(JOURNAL_MAGIC)
        outputStream.writeByte(JOURNAL_VERSION.toInt())
        outputStream.flush()
    }

    fun writeDirty(key: String) = writeOperation(JournalOp.Dirty(key))

    fun writeClean(key: String, length: Int) = writeOperation(JournalOp.Clean(key, length))

    fun writeRemove(key: String) = writeOperation(JournalOp.Remove(key))

    fun writeOperation(operation: JournalOp) {
        when (operation) {
            is JournalOp.Dirty -> outputStream.run {
                writeByte(OPCODE_DIRTY.toInt())
                writeLengthString(operation.key)
            }

            is JournalOp.Clean -> outputStream.run {
                writeByte(OPCODE_CLEAN.toInt())
                writeLengthString(operation.key)
                writeInt(operation.length)
            }

            is JournalOp.Remove -> outputStream.run {
                writeByte(OPCODE_REMOVE.toInt())
                writeLengthString(operation.key)
            }
        }

        outputStream.flush()
    }

    override fun close() {
        outputStream.close()
    }
}


class JournalReader private constructor(journalFile: File) : Closeable {
    private val inputStream = DataInputStream(journalFile.inputStream())

    fun readFully(): List<JournalOp> {
        val result = mutableListOf<JournalOp>()

        validateHeader()
        while (inputStream.available() > 0) {
            val operation = readOperation()
            if (operation != null) result += operation
        }

        return result
    }

    fun validateHeader() {
        val magic = inputStream.readString(JOURNAL_MAGIC.length)
        val version = inputStream.readByte()

        check(magic == JOURNAL_MAGIC) { "Journal magic string doesn't match" }
        check(version == JOURNAL_VERSION) { "Journal version doesn't match" }
    }

    fun readOperation(): JournalOp? {
        return try {
            val opcode = inputStream.readByte()
            val key = inputStream.readString()
            when (opcode) {
                OPCODE_DIRTY -> JournalOp.Dirty(key)
                OPCODE_CLEAN -> JournalOp.Clean(key, inputStream.readInt())
                OPCODE_REMOVE -> JournalOp.Remove(key)
                else -> null
            }
        } catch (ex: IOException) {
            null
        }
    }

    override fun close() {
        inputStream.close()
    }
}

sealed interface JournalOp {
    val opcode: Byte

    val key: String

    data class Dirty(override val key: String) : JournalOp {
        override val opcode = OPCODE_DIRTY
    }

    data class Clean(override val key: String, val length: Int) : JournalOp {
        override val opcode = OPCODE_CLEAN
    }

    data class Remove(override val key: String) : JournalOp {
        override val opcode = OPCODE_REMOVE
    }
}


private fun DataOutputStream.writeLengthString(string: String) {
    writeByte(string.length)
    writeString(string)
}

private fun DataOutputStream.writeString(string: String) =
    write(string.encodeToByteArray())


private fun DataInputStream.readString() =
    readString(readByte().toInt())

private fun DataInputStream.readString(length: Int) =
    readBytes(length).decodeToString()

private fun DataInputStream.readBytes(count: Int) =
    ByteArray(count).also { read(it) }

private const val BUFFER_SIZE = 256 // Flushing after each operation

private const val JOURNAL_MAGIC = "JOURNAL"
private const val JOURNAL_VERSION: Byte = 1

private const val OPCODE_DIRTY: Byte = 1
private const val OPCODE_CLEAN: Byte = 2
private const val OPCODE_REMOVE: Byte = 3