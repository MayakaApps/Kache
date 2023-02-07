package com.mayakapps.lrucache.journal

internal const val JOURNAL_MAGIC = "JOURNAL"
internal const val JOURNAL_VERSION = 1

internal const val OPCODE_DIRTY = 1
internal const val OPCODE_CLEAN = 2
internal const val OPCODE_REMOVE = 3