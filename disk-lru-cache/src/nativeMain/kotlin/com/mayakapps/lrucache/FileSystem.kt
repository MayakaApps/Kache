package com.mayakapps.lrucache

import okio.FileSystem

internal actual fun getSystemFileSystem(): FileSystem = FileSystem.SYSTEM