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

import okio.BufferedSink
import okio.Closeable

internal class JournalWriter(
    private val sink: BufferedSink,
    private val cacheVersion: Int = 1,
) : Closeable {

    internal fun writeHeader() {
        sink.writeUtf8(JOURNAL_MAGIC)
        sink.writeByte(JOURNAL_VERSION.toInt())
        sink.writeInt(cacheVersion)
        sink.flush()
    }

    internal fun writeAll(cleanKeys: Collection<String>, dirtyKeys: Collection<String>) {
        for (key in cleanKeys) writeEntry(JournalEntry.CLEAN, key)
        for (key in dirtyKeys) writeEntry(JournalEntry.DIRTY, key)
        sink.flush()
    }

    internal fun writeDirty(key: String) = writeEntryAndFlush(JournalEntry.DIRTY, key)

    internal fun writeClean(key: String) = writeEntryAndFlush(JournalEntry.CLEAN, key)

    internal fun writeCancel(key: String) = writeEntryAndFlush(JournalEntry.CANCEL, key)

    internal fun writeRemove(key: String) = writeEntryAndFlush(JournalEntry.REMOVE, key)

    internal fun writeRead(key: String) = writeEntryAndFlush(JournalEntry.READ, key)

    private fun writeEntryAndFlush(opcode: Byte, key: String) {
        writeEntry(opcode, key)
        sink.flush()
    }

    private fun writeEntry(opcode: Byte, key: String) {
        sink.writeByte(opcode.toInt())
        sink.writeByteLengthUtf8(key)
    }

    private fun BufferedSink.writeByteLengthUtf8(string: String) {
        writeByte(string.length)
        writeUtf8(string)
    }

    override fun close() {
        sink.close()
    }
}
