package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import okio.Closeable
import okio.FileSystem
import okio.Path
import okio.use

// Using variables instead of functions cause error in native compilation. It is a bug in Kotlin/Native.

internal expect fun getDefaultFileSystem(): FileSystem

internal expect fun getIODispatcher(): CoroutineDispatcher