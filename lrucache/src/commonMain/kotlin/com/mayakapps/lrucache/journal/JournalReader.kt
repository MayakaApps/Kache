package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.Closeable
import com.mayakapps.lrucache.io.IOException
import com.mayakapps.lrucache.io.InputStream

internal class JournalReader(private val inputStream: InputStream) : Closeable {

    fun readJournal(): Result {
        var isCorrupted = false
        val keys = mutableListOf<String>()
        var opsCount = 0

        // Validate magic code
        try {
            validateHeader()
        } catch (ex: IOException) {
            isCorrupted = true
            return Result(isCorrupted, keys, 0)
        } catch (ex: IllegalStateException) {
            isCorrupted = true
            return Result(isCorrupted, keys, 0)
        }

        // Read operations
        try {
            while (true) when (inputStream.read()) {
                -1 -> break // EOF

                OPCODE_DIRTY -> {
                    inputStream.skipString()
                    opsCount++
                }

                OPCODE_CLEAN -> {
                    keys += inputStream.readString()
                    opsCount++
                }

                OPCODE_REMOVE -> {
                    keys.remove(inputStream.readString())
                    opsCount++
                }

                else -> {
                    isCorrupted = true
                    break
                }
            }
        } catch (ex: IOException) {
            isCorrupted = true
        }

        return Result(isCorrupted, keys, opsCount)
    }

    private fun validateHeader() {
        val magic = inputStream.readString(JOURNAL_MAGIC.length)
        val version = inputStream.read()

        check(magic == JOURNAL_MAGIC) { "Journal magic string doesn't match" }
        check(version == JOURNAL_VERSION) { "Journal version doesn't match" }
    }

    override fun close() {
        inputStream.close()
    }

    // Read Helpers

    private fun InputStream.readString(): String {
        val length = read()
        return if (length != -1) readString(length)
        else throw IOException("Corrupted journal")
    }

    private fun InputStream.skipString() {
        val length = read()
        if (length != -1) readBytes(length)
        else throw IOException("Corrupted journal")
    }

    private fun InputStream.readString(length: Int) = readBytes(length).decodeToString()

    private fun InputStream.readBytes(count: Int) =
        ByteArray(count).also { if (read(it) != count) throw IOException("Corrupted journal") }

    // Result

    data class Result(
        val isCorrupted: Boolean,
        val cleanKeys: List<String>,
        val opsCount: Int,
    )
}