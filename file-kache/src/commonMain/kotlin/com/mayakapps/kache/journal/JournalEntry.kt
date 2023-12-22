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