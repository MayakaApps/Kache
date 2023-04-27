@file:JvmName("FileSystemJvmKt")

package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import okio.FileSystem

internal actual val defaultFileSystem: FileSystem = FileSystem.SYSTEM

actual val ioDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO