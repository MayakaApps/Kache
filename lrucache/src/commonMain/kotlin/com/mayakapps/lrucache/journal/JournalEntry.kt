package com.mayakapps.lrucache.journal

internal sealed interface JournalEntry {
    val key: String

    data class Dirty(override val key: String) : JournalEntry

    data class Clean(override val key: String) : JournalEntry

    data class Remove(override val key: String) : JournalEntry

    companion object {
        operator fun invoke(opcode: Int, key: String) = when (opcode) {
            DIRTY -> Dirty(key)
            CLEAN -> Clean(key)
            REMOVE -> Remove(key)
            else -> throw JournalInvalidOpcodeException()
        }

        const val DIRTY = 1
        const val CLEAN = 2
        const val REMOVE = 3
    }
}