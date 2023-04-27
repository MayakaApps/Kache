package com.mayakapps.kache.journal

import okio.IOException

internal sealed class JournalException(override val message: String? = null) : IOException(message)

internal class JournalInvalidHeaderException(override val message: String? = null) : JournalException(message)

// Message property is required as a workaround for https://youtrack.jetbrains.com/issue/KT-43490
internal class JournalInvalidOpcodeException(override val message: String? = null) : JournalException(message)