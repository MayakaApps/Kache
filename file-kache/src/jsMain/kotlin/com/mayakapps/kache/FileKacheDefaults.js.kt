package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.NodeJsFileSystem

internal actual object FileKacheDefaults {
    actual val defaultFileSystem: FileSystem = NodeJsFileSystem
    actual val defaultCoroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
}
