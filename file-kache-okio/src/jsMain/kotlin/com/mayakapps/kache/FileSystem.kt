package com.mayakapps.kache

import okio.FileSystem
import okio.NodeJsFileSystem

internal actual fun getSystemFileSystem(): FileSystem = NodeJsFileSystem