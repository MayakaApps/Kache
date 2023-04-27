package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.NodeJsFileSystem

internal actual val defaultFileSystem: FileSystem = NodeJsFileSystem

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default