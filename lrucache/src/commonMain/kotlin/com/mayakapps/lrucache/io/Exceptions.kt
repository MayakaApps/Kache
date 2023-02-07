package com.mayakapps.lrucache.io

internal expect open class IOException(message: String? = null, cause: Throwable? = null): Exception

internal expect open class FileNotFoundException(message: String? = null): IOException