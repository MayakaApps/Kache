/*
 * Copyright 2023 MayakaApps
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

import okio.BufferedSource
import okio.Closeable
import okio.EOFException

internal class JournalReader(
    private val source: BufferedSource,
    private val cacheVersion: Int = 1,
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
        if (cacheVersion != existingCacheVersion)
            throw JournalInvalidHeaderException("Existing cache version ($existingCacheVersion) doesn't match current version ($cacheVersion)")
    }

    internal fun readEntry(): JournalEntry? {
        val opcodeId = try {
            source.readByte()
        } catch (ex: EOFException) {
            // Fine, we've reached the end of the file
            return null
        }

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
