package com.mayakapps.lrucache.io

internal interface FileManager {

    fun exists(file: File): Boolean

    fun isDirectory(file: File): Boolean

    fun size(file: File): Long

    fun listContent(file: File): List<File>?

    fun delete(file: File): Boolean

    fun deleteRecursively(file: File): Boolean

    fun renameTo(oldFile: File, newFile: File): Boolean

    fun mkdirs(file: File): Boolean
}

internal expect object DefaultFileManager: FileManager

internal fun FileManager.renameToOrThrow(oldFile: File, newFile: File, deleteDest: Boolean) {
    if (deleteDest) deleteOrThrow(newFile)
    if (!renameTo(oldFile, newFile)) throw IOException()
}

internal fun FileManager.deleteOrThrow(file: File) {
    if (exists(file) && !delete(file)) throw IOException()
}