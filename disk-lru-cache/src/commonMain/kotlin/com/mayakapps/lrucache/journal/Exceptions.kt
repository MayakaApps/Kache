package com.mayakapps.lrucache.journal

import com.mayakapps.lrucache.io.IOException

internal sealed class JournalException(override val message: String? = null) : IOException(message)

internal class JournalInvalidHeaderException(override val message: String? = null) : JournalException(message)

internal class JournalInvalidOpcodeException : JournalException()