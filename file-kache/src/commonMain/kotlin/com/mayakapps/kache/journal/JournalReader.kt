/*
 * Copyright 2024 MayakaApps
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mayakapps.kache.journal

import com.mayakapps.kache.KacheStrategy
import okio.BufferedSource
import okio.Closeable
import okio.EOFException

internal class JournalReader(
    private val source: BufferedSource,
    private val cacheVersion: Int = 1,
    private val strategy: KacheStrategy = KacheStrategy.LRU,
) : Closeable {

    internal fun validateHeader() {
        val magic = try {
            source.readUtf8(JOURNAL_MAGIC.length.toLong())
        } catch (ex: EOFException) {
            throw JournalInvalidHeaderException("File size is less than journal magic code size")
        }

        if (magic != JOURNAL_MAGIC) throw JournalInvalidHeaderException("Journal magic ($magic) doesn't match")

        val version = source.readByte()
        if (version != JOURNAL_VERSION) throw JournalInvalidHeaderException("Journal version ($version) doesn't match")

        val existingCacheVersion = source.readInt()
        if (cacheVersion != existingCacheVersion) {
            throw JournalInvalidHeaderException(
                "Existing cache version ($existingCacheVersion) doesn't match current version ($cacheVersion)"
            )
        }

        val existingStrategy = source.readByte()
        if (strategy.ordinal != existingStrategy.toInt()) {
            throw JournalInvalidHeaderException(
                "Existing strategy ($existingStrategy) doesn't match current strategy (${strategy.ordinal})"
            )
        }
    }

    internal fun readEntry(): JournalEntry? {
        val opcodeId = try {
            source.readByte()
        } catch (ex: EOFException) {
            // Fine, we've reached the end of the file
            return null
        }

        val key = source.readUShortLengthUtf8()

        val transformedKey =
            if (opcodeId == JournalEntry.CLEAN_WITH_TRANSFORMED_KEY) source.readUByteLengthUtf8()
            else null

        return JournalEntry(opcodeId, key, transformedKey)
    }

    override fun close() {
        source.close()
    }

    private fun BufferedSource.readUByteLengthUtf8(): String {
        val length = readByte().toUByte().toLong()
        return readUtf8(length)
    }

    private fun BufferedSource.readUShortLengthUtf8(): String {
        val length = readShort().toUShort().toLong()
        return readUtf8(length)
    }
}
