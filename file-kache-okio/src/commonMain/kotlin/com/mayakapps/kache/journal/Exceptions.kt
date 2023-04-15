package com.mayakapps.kache.journal

import okio.IOException

internal sealed class JournalException(override val message: String? = null) : IOException(message)

internal class JournalInvalidHeaderException(override val message: String? = null) : JournalException(message)

// message property is required as a workaround for KT-43490
// TODO: Remove message property when the issue is fixed, most likely in Kotlin 1.9.0
internal class JournalInvalidOpcodeException(override val message: String? = null) : JournalException(message)