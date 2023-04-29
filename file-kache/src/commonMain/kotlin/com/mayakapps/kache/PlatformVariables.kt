package com.mayakapps.kache

import kotlinx.coroutines.CoroutineDispatcher

// Using functions instead of properties as a workaround for https://youtrack.jetbrains.com/issue/KT-47144

internal expect fun getIODispatcher(): CoroutineDispatcher