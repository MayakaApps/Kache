package com.mayakapps.lrucache.io

internal expect class BufferedInputStream(base: InputStream, size: Int = 8192) : InputStream