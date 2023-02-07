package com.mayakapps.lrucache.io

internal expect class FileOutputStream(path: String, append: Boolean = true) : OutputStream