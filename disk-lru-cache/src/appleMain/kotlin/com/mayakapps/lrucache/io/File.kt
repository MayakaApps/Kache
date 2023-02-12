package com.mayakapps.lrucache.io

import platform.Foundation.NSString
import platform.Foundation.pathWithComponents

// Workaround for a bug: https://youtrack.jetbrains.com/issue/KT-37316
@Suppress("ACTUAL_WITHOUT_EXPECT")
internal actual typealias File = String

internal actual inline fun getFile(path: String) = path

internal actual inline fun getFile(parent: File, child: String) = NSString.pathWithComponents(listOf(parent, child))

internal actual inline fun File.appendExt(ext: String) = this + ext

internal actual val File.filePath: String get() = this