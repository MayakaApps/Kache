package com.mayakapps.lrucache.io

import kotlinx.cinterop.*
import platform.Foundation.NSFileManager

internal actual object FileManager {

    private val fileManager = NSFileManager.defaultManager

    actual fun exists(file: File): Boolean = fileManager.fileExistsAtPath(file)

    actual fun isDirectory(file: File): Boolean {
        return memScoped {
            val resultVar = alloc<BooleanVar>()
            fileManager.fileExistsAtPath(file, resultVar.ptr)
            resultVar.value
        }
    }

    actual fun delete(file: File): Boolean = fileManager.removeItemAtPath(file, null)

    actual fun deleteRecursively(file: File): Boolean = fileManager.removeItemAtPath(file, null)

    actual fun renameTo(oldFile: File, newFile: File): Boolean =
        fileManager.moveItemAtPath(oldFile, newFile, null)

    actual fun size(file: File): Long =
        (fileManager.attributesOfItemAtPath(file, null)?.get("size") as Long?) ?: -1

    @Suppress("UNCHECKED_CAST")
    actual fun listContent(file: File): List<String>? =
        fileManager.contentsOfDirectoryAtPath(file, null) as List<File>?

    actual fun mkdirs(file: File): Boolean =
        fileManager.createDirectoryAtPath(file, withIntermediateDirectories = true, null, null)
}