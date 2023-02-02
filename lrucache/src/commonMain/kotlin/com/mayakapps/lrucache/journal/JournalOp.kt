package com.mayakapps.lrucache.journal

internal sealed interface JournalOp {
    val key: String

    data class Dirty(override val key: String) : JournalOp
    data class Clean(override val key: String) : JournalOp
    data class Remove(override val key: String) : JournalOp

    val opcode: Byte
        get() = when (this) {
            is Dirty -> OPCODE_DIRTY
            is Clean -> OPCODE_CLEAN
            is Remove -> OPCODE_REMOVE
        }

    companion object {
        fun create(opcode: Byte, key: String) =
            when (opcode) {
                OPCODE_DIRTY -> Dirty(key)
                OPCODE_CLEAN -> Clean(key)
                OPCODE_REMOVE -> Remove(key)
                else -> throw IllegalArgumentException()
            }

        private const val OPCODE_DIRTY: Byte = 1
        private const val OPCODE_CLEAN: Byte = 2
        private const val OPCODE_REMOVE: Byte = 3
    }
}