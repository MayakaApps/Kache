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

import okio.*
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class JournalWriteTests {

    @Test
    fun testWriteHeader() {
        val buffer = Buffer()
        JournalWriter(buffer).use { it.writeHeader() }
        assertContentEquals(headerBytes, buffer.readByteArray())
    }

    @Test
    fun testWriteDirty() {
        val bytes = byteArrayOf(
            JournalEntry.DIRTY, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val buffer = Buffer()
        JournalWriter(buffer).use { it.writeDirty(KEY) }
        assertContentEquals(bytes, buffer.readByteArray())
    }

    @Test
    fun testWriteClean() {
        val bytes = byteArrayOf(
            JournalEntry.CLEAN, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val buffer = Buffer()
        JournalWriter(buffer).use { it.writeClean(KEY) }
        assertContentEquals(bytes, buffer.readByteArray())
    }

    @Test
    fun testWriteRemove() {
        val bytes = byteArrayOf(
            JournalEntry.REMOVE, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val buffer = Buffer()
        JournalWriter(buffer).use { it.writeRemove(KEY) }
        assertContentEquals(bytes, buffer.readByteArray())
    }

    @Test
    fun testWriteAll() {
        val bytes = byteArrayOf(
            JournalEntry.CLEAN, 0x07, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
            JournalEntry.DIRTY, 0x0A, 0x41, 0x6C, 0x74, 0x54, 0x65, 0x73, 0x74, 0x4B, 0x65, 0x79,
        )

        val buffer = Buffer()
        JournalWriter(buffer).use { it.writeAll(listOf(KEY), listOf(ALT_KEY)) }
        assertContentEquals(bytes, buffer.readByteArray())
    }

    @Test
    fun testClose() {
        val sink = object : Sink {
            var wasClosed = false

            override fun close() {
                wasClosed = true
            }

            override fun write(source: Buffer, byteCount: Long) {}
            override fun timeout(): Timeout = Timeout.NONE
            override fun flush() {}
        }

        JournalWriter(sink.buffer()).close()
        assertTrue(sink.wasClosed)
    }

    companion object {
        private const val KEY = "TestKey"
        private const val ALT_KEY = "AltTestKey"
        private val headerBytes =
            JOURNAL_MAGIC.encodeToByteArray() + JOURNAL_VERSION + byteArrayOf(0x00, 0x00, 0x00, 0x01)
    }
}
