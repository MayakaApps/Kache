@file:JvmName("FileSystemJvmKt")

package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import okio.FileSystem

internal actual fun getDefaultFileSystem(): FileSystem = FileSystem.SYSTEM

actual fun getIODispatcher(): CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO