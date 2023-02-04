package com.mayakapps.lrucache

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

internal fun File.renameToOrThrow(dest: File, deleteDest: Boolean) {
    if (deleteDest) dest.deleteOrThrow()
    if (!renameTo(dest)) throw IOException()
}

internal fun File.deleteOrThrow() {
    if (exists() && !delete()) throw IOException()
}