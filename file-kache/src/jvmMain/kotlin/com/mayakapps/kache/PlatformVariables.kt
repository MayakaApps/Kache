@file:JvmName("FileSystemJvmKt")

package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher

internal actual val ioDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO