package com.mayakapps.lrucache.io

internal expect class File

internal expect inline fun getFile(path: String): File

internal expect inline fun getFile(parent: File, child: String): File

internal expect inline fun File.appendExt(ext: String): File

internal expect val File.filePath: String