package com.mayakapps.lrucache.journal

internal sealed interface JournalEntry {
    val key: String

    data class Dirty(override val key: String) : JournalEntry

    data class Clean(override val key: String) : JournalEntry

    data class Remove(override val key: String) : JournalEntry

    val opcode: Byte
        get() = when (this) {
            is Dirty -> DIRTY
            is Clean -> CLEAN
            is Remove -> REMOVE
        }

    companion object {
        operator fun invoke(opcode: Byte, key: String) = when (opcode) {
            DIRTY -> Dirty(key)
            CLEAN -> Clean(key)
            REMOVE -> Remove(key)
            else -> throw JournalInvalidOpcodeException()
        }

        const val DIRTY: Byte = 1
        const val CLEAN: Byte = 2
        const val REMOVE: Byte = 3
    }
}