package com.mayakapps.kache

import okio.FileSystem

internal actual fun getSystemFileSystem(): FileSystem = FileSystem.SYSTEM