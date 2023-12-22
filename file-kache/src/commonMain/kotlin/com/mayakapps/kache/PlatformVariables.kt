package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import okio.FileSystem

// Using functions instead of properties as a workaround for https://youtrack.jetbrains.com/issue/KT-47144

internal expect fun getDefaultFileSystem(): FileSystem

internal expect fun getIODispatcher(): CoroutineDispatcher