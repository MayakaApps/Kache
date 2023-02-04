package com.mayakapps.lrucache.io

import kotlinx.cinterop.*
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.pathWithComponents

internal actual object Files {

    private val fileManager = NSFileManager.defaultManager

    actual fun appendPath(parent: String, child: String): String =
        NSString.pathWithComponents(listOf(parent, child))

    actual fun exists(path: String): Boolean = fileManager.fileExistsAtPath(path)

    actual fun isDirectory(path: String): Boolean {
        return memScoped {
            val resultVar = alloc<BooleanVar>()
            fileManager.fileExistsAtPath(path, resultVar.ptr)
            resultVar.value
        }
    }

    actual fun delete(path: String): Boolean = fileManager.removeItemAtPath(path, null)

    actual fun deleteRecursively(path: String): Boolean = fileManager.removeItemAtPath(path, null)

    actual fun renameTo(oldPath: String, newPath: String): Boolean =
        fileManager.moveItemAtPath(oldPath, newPath, null)

    actual fun size(path: String): Long =
        (fileManager.attributesOfItemAtPath(path, null)?.get("size") as Long?) ?: -1

    @Suppress("UNCHECKED_CAST")
    actual fun listContent(path: String): List<String>? =
        fileManager.contentsOfDirectoryAtPath(path, null) as List<String>?

    actual fun mkdirs(path: String): Boolean =
        fileManager.createDirectoryAtPath(path, withIntermediateDirectories = true, null, null)
}