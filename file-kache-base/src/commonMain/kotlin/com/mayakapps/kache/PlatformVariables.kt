package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import okio.FileSystem

internal expect val defaultFileSystem: FileSystem

expect val ioDispatcher: CoroutineDispatcher