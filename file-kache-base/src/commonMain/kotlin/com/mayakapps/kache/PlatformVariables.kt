package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import okio.Closeable
import okio.FileSystem
import okio.Path
import okio.use

internal expect val defaultFileSystem: FileSystem

expect val ioDispatcher: CoroutineDispatcher