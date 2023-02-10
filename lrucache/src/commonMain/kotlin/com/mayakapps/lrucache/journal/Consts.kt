package com.mayakapps.lrucache.journal

internal const val JOURNAL_MAGIC = "JOURNAL"
internal const val JOURNAL_VERSION = 1

internal enum class Opcode(val id: Int) {
    DIRTY(1), CLEAN(2), REMOVE(3);

    companion object {
        private val opcodes = values()

        fun fromId(id: Int) = opcodes.find { it.id == id } ?: throw IllegalArgumentException()
    }
}