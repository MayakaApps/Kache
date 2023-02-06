package com.mayakapps.lrucache.io

import java.io.File

internal actual object FileManager {

    actual fun exists(file: File): Boolean = file.exists()

    actual fun isDirectory(file: File): Boolean = file.isDirectory

    actual fun size(file: File) = file.length()

    actual fun listContent(file: File): List<File>? = file.listFiles()?.toList()

    actual fun delete(file: File): Boolean = file.delete()

    actual fun deleteRecursively(file: File): Boolean = file.deleteRecursively()

    actual fun renameTo(oldFile: File, newFile: File): Boolean = oldFile.renameTo(newFile)

    actual fun mkdirs(file: File): Boolean = file.mkdirs()
}