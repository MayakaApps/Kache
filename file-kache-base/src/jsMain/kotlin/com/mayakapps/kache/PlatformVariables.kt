package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.NodeJsFileSystem

internal actual fun getDefaultFileSystem(): FileSystem = NodeJsFileSystem

actual fun getIODispatcher(): CoroutineDispatcher = Dispatchers.Default