package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.Closeable
import com.mayakapps.lrucache.io.InputStream

internal class JournalReader(private val inputStream: InputStream) : Closeable {

    fun readJournal(): Result {
        val keys = mutableListOf<String>()
        var opsCount = 0

        validateHeader()

        // Read operations
        while (true) {
            val opcodeId = inputStream.read()
            if (opcodeId == -1) break // Expected EOF

            val opcode = try {
                Opcode.fromId(opcodeId)
            } catch (ex: IllegalArgumentException) {
                throw JournalInvalidOpcodeException()
            }

            when (opcode) {
                Opcode.DIRTY -> {
                    inputStream.readString()
                    opsCount++
                }

                Opcode.CLEAN -> {
                    keys += inputStream.readString()
                    opsCount++
                }

                Opcode.REMOVE -> {
                    keys.remove(inputStream.readString())
                    opsCount++
                }
            }
        }

        return Result(keys, opsCount)
    }

    private fun validateHeader() {
        val magic = try {
            inputStream.readString(JOURNAL_MAGIC.length)
        } catch (ex: JournalEOFException) {
            throw JournalInvalidHeaderException("File size is less than journal magic code size")
        }

        val version = inputStream.read()

        if (magic != JOURNAL_MAGIC) throw JournalInvalidHeaderException("Journal magic ($magic) doesn't match")
        if (version != JOURNAL_VERSION) throw JournalInvalidHeaderException("Journal version ($version) doesn't match")
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

    // Result

    data class Result(
        val cleanKeys: List<String>,
        val opsCount: Int,
    )
}