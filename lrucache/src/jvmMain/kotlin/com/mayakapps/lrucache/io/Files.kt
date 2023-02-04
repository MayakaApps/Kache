package com.mayakapps.lrucache.io

import java.io.File

internal actual object Files {

    actual fun appendPath(parent: String, child: String): String = File(parent, child).path

    actual fun exists(path: String): Boolean = File(path).exists()

    actual fun isDirectory(path: String): Boolean = File(path).isDirectory

    actual fun size(path: String) = File(path).length()

    actual fun listContent(path: String): List<String>? = File(path).list()?.toList()

    actual fun delete(path: String): Boolean = File(path).delete()

    actual fun deleteRecursively(path: String): Boolean = File(path).deleteRecursively()

    actual fun renameTo(oldPath: String, newPath: String): Boolean = File(oldPath).renameTo(File(newPath))

    actual fun mkdirs(path: String): Boolean = File(path).mkdirs()
}