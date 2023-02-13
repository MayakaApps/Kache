package com.mayakapps.lrucache.io

internal actual open class IOException actual constructor(message: String? = null, cause: Throwable? = null) :
    Exception(message, cause)

internal actual open class FileNotFoundException actual constructor(message: String? = null) : IOException(message)