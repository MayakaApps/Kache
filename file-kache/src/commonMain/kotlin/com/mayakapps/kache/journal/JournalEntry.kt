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

internal sealed interface JournalEntry {
    val key: String

    data class Dirty(override val key: String) : JournalEntry

    data class Clean(override val key: String) : JournalEntry

    data class Cancel(override val key: String) : JournalEntry

    data class Remove(override val key: String) : JournalEntry

    data class Read(override val key: String) : JournalEntry

    val opcode: Byte
        get() = when (this) {
            is Dirty -> DIRTY
            is Clean -> CLEAN
            is Cancel -> CANCEL
            is Remove -> REMOVE
            is Read -> READ
        }

    companion object {
        operator fun invoke(opcode: Byte, key: String) = when (opcode) {
            DIRTY -> Dirty(key)
            CLEAN -> Clean(key)
            CANCEL -> Cancel(key)
            REMOVE -> Remove(key)
            READ -> Read(key)
            else -> throw JournalInvalidOpcodeException()
        }

        const val DIRTY: Byte = 1
        const val CLEAN: Byte = 2
        const val CANCEL: Byte = 3
        const val REMOVE: Byte = 4
        const val READ: Byte = 5
    }
}
