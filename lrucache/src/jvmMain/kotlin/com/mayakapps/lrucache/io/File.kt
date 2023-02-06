@file:Suppress("NOTHING_TO_INLINE")

package com.mayakapps.lrucache.io

// Workaround for a bug: https://youtrack.jetbrains.com/issue/KT-37316
@Suppress("ACTUAL_WITHOUT_EXPECT")
internal actual typealias File = java.io.File

internal actual inline fun getFile(path: String) = java.io.File(path)

internal actual inline fun getFile(parent: File, child: String) = java.io.File(parent, child)

internal actual inline fun File.appendExt(ext: String) = java.io.File(path + ext)

internal actual val File.filePath: String get() = path