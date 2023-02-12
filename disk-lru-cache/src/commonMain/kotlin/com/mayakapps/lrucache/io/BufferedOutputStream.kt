package com.mayakapps.lrucache.io

internal expect class BufferedOutputStream(base: OutputStream, size: Int = 8192) : OutputStream