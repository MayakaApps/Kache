package com.mayakapps.lrucache.io

internal expect object Files {

    fun appendPath(parent: String, child: String): String

    fun exists(path: String): Boolean

    fun isDirectory(path: String): Boolean

    fun size(path: String): Long

    fun listContent(path: String): List<String>?

    fun delete(path: String): Boolean

    fun deleteRecursively(path: String): Boolean

    fun renameTo(oldPath: String, newPath: String): Boolean

    fun mkdirs(path: String): Boolean
}

internal fun Files.renameToOrThrow(oldPath: String, newPath: String, deleteDest: Boolean) {
    if (deleteDest) deleteOrThrow(newPath)
    if (!renameTo(oldPath, newPath)) throw IOException()
}

internal fun Files.deleteOrThrow(path: String) {
    if (exists(path) && !delete(path)) throw IOException()
}