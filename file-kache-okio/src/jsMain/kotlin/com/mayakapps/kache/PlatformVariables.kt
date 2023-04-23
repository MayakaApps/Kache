package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.NodeJsFileSystem

internal actual fun getDefaultFileSystem(): FileSystem = NodeJsFileSystem

internal actual fun getIODispatcher(): CoroutineDispatcher = Dispatchers.Default