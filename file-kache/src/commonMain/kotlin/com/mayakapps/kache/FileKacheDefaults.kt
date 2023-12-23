package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import okio.FileSystem

internal expect object FileKacheDefaults {
    val defaultFileSystem: FileSystem
    val defaultCoroutineDispatcher: CoroutineDispatcher
}
