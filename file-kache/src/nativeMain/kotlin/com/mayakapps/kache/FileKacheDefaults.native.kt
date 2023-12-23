package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.FileSystem

internal actual object FileKacheDefaults {
    actual val defaultFileSystem: FileSystem = FileSystem.SYSTEM
    actual val defaultCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
}
