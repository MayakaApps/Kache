package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.Closeable
import com.mayakapps.lrucache.writeLengthString
import com.mayakapps.lrucache.writeString
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

internal actual class JournalWriter(journalFile: File, append: Boolean = true) : Closeable {

    actual constructor(path: String, append: Boolean) : this(File(path), append)

    private val outputStream =
        DataOutputStream(FileOutputStream(journalFile, append).buffered(BUFFER_SIZE))

    actual fun writeHeader() {
        outputStream.writeString(JOURNAL_MAGIC)
        outputStream.writeByte(JOURNAL_VERSION.toInt())
        outputStream.flush()
    }

    actual fun writeOperation(operation: JournalOp) {
        outputStream.run {
            writeByte(operation.opcode.toInt())
            writeLengthString(operation.key)
        }

        outputStream.flush()
    }

    override fun close() {
        outputStream.close()
    }

    companion object {
        // This is more than enough as the buffer is flushing after each operation
        private const val BUFFER_SIZE = 256
    }
}