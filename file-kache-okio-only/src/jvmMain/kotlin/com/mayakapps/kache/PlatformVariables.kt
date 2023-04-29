@file:JvmName("FileSystemJvmKt")

package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import okio.FileSystem

internal actual val defaultFileSystem: FileSystem = FileSystem.SYSTEM

internal actual val ioDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO