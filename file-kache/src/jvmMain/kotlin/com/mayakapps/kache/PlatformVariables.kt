@file:JvmName("FileSystemJvmKt")

package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher

internal actual fun getIODispatcher(): CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO