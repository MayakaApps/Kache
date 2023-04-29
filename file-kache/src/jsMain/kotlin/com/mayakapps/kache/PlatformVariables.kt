package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual fun getIODispatcher(): CoroutineDispatcher = Dispatchers.Default