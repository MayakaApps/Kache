package com.mayakapps.lrucache

import java.io.File
import java.io.IOException

internal class CachedFile(file: File) : File(file.path) {
    override fun delete(): Boolean = throwError()
    override fun deleteOnExit() = throwError()
    override fun renameTo(dest: File?): Boolean = throwError()

    private fun throwError(): Nothing =
        throw IOException("Cached files cannot be manually moved/deleted")
}