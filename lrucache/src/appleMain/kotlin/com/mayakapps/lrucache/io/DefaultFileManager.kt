package com.mayakapps.lrucache.io

import kotlinx.cinterop.*
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize

internal actual object DefaultFileManager : FileManager {

    private val fileManager = NSFileManager.defaultManager

    override fun exists(file: File): Boolean = fileManager.fileExistsAtPath(file)

    override fun isDirectory(file: File): Boolean {
        return memScoped {
            val resultVar = alloc<BooleanVar>()
            fileManager.fileExistsAtPath(file, resultVar.ptr)
            resultVar.value
        }
    }

    override fun size(file: File): Long =
        (fileManager.attributesOfItemAtPath(file, null)?.get(NSFileSize) as Long?) ?: 0

    @Suppress("UNCHECKED_CAST")
    override fun listContent(file: File): List<String>? =
        fileManager.contentsOfDirectoryAtPath(file, null) as List<File>?

    override fun delete(file: File): Boolean = fileManager.removeItemAtPath(file, null)

    override fun deleteRecursively(file: File): Boolean = fileManager.removeItemAtPath(file, null)

    override fun renameTo(oldFile: File, newFile: File): Boolean =
        fileManager.moveItemAtPath(oldFile, newFile, null)

    override fun mkdirs(file: File): Boolean =
        fileManager.createDirectoryAtPath(file, withIntermediateDirectories = true, null, null)
}