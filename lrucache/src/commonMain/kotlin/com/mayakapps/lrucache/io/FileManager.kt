package com.mayakapps.lrucache.io

internal interface FileManager {

    fun exists(file: File): Boolean

    fun isDirectory(file: File): Boolean

    fun size(file: File): Long

    fun listContents(file: File): List<File>?

    fun delete(file: File): Boolean

    fun deleteOrThrow(file: File) {
        if (exists(file) && !delete(file)) throw IOException()
    }

    fun deleteRecursively(file: File): Boolean

    fun deleteContentsOrThrow(file: File) {
        val contents = listContents(file) ?: throw IOException("Not a readable directory: ${file.filePath}")
        for (subFile in contents) {
            if (isDirectory(subFile)) deleteContentsOrThrow(subFile)
            if (!delete(file)) throw IOException()
        }
    }

    fun renameTo(oldFile: File, newFile: File): Boolean

    fun renameToOrThrow(oldFile: File, newFile: File, deleteDest: Boolean) {
        if (deleteDest) deleteOrThrow(newFile)
        if (!renameTo(oldFile, newFile)) throw IOException()
    }

    fun createDirectories(file: File): Boolean
}

internal expect object DefaultFileManager : FileManager

