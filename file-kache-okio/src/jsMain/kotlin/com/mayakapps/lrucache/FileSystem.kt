package com.mayakapps.lrucache

import okio.FileSystem
import okio.NodeJsFileSystem

internal actual fun getSystemFileSystem(): FileSystem = NodeJsFileSystem