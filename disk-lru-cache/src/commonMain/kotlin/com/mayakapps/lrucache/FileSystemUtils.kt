package com.mayakapps.lrucache

import okio.FileSystem
import okio.Path

fun FileSystem.deleteContents(dir: Path) {
    list(dir).forEach { deleteRecursively(it) }
}

fun FileSystem.atomicMove(source: Path, target: Path, deleteTarget: Boolean) {
    if (deleteTarget) delete(target)
    atomicMove(source, target)
}