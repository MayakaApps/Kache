package com.mayakapps.lrucache.io

import java.io.File

internal actual object DefaultFileManager : FileManager {

    override fun exists(file: File): Boolean = file.exists()

    override fun isDirectory(file: File): Boolean = file.isDirectory

    override fun size(file: File) = file.length()

    override fun listContents(file: File): List<File>? = file.listFiles()?.toList()

    override fun delete(file: File): Boolean = file.delete()

    override fun deleteRecursively(file: File): Boolean = file.deleteRecursively()

    override fun renameTo(oldFile: File, newFile: File): Boolean = oldFile.renameTo(newFile)

    override fun createDirectories(file: File): Boolean = file.mkdirs() || file.isDirectory
}