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

internal fun JournalEntry(
    opcode: Byte,
    key: String,
    transformedKey: String? = null,
) = when (opcode) {
    JournalEntry.DIRTY -> JournalEntry.Dirty(key)
    JournalEntry.CLEAN -> JournalEntry.Clean(key)
    JournalEntry.CLEAN_WITH_TRANSFORMED_KEY -> JournalEntry.CleanWithTransformedKey(key, transformedKey!!)
    JournalEntry.CANCEL -> JournalEntry.Cancel(key)
    JournalEntry.REMOVE -> JournalEntry.Remove(key)
    JournalEntry.READ -> JournalEntry.Read(key)

    else -> throw JournalInvalidOpcodeException()
}

internal sealed interface JournalEntry {
    val key: String

    data class Dirty(override val key: String) : JournalEntry
    data class Clean(override val key: String) : JournalEntry
    data class CleanWithTransformedKey(override val key: String, val transformedKey: String) : JournalEntry
    data class Cancel(override val key: String) : JournalEntry
    data class Remove(override val key: String) : JournalEntry
    data class Read(override val key: String) : JournalEntry

    companion object {
        const val DIRTY: Byte = 0x10
        const val CLEAN: Byte = 0x20
        const val CLEAN_WITH_TRANSFORMED_KEY: Byte = 0x21
        const val CANCEL: Byte = 0x30
        const val REMOVE: Byte = 0x40
        const val READ: Byte = 0x50
    }
}
