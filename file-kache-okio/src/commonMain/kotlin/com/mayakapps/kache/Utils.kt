package com.mayakapps.kache

import okio.Closeable
import okio.FileSystem
import okio.Path
import okio.use

internal fun FileSystem.deleteContents(dir: Path) {
    list(dir).forEach { deleteRecursively(it) }
}

internal fun FileSystem.atomicMove(source: Path, target: Path, deleteTarget: Boolean) {
    if (deleteTarget) delete(target)
    atomicMove(source, target)
}

internal inline fun <T : Closeable?, R> T.nullableUse(block: (T) -> R?): R? = try {
    use(block)
} catch (ex: NullPointerException) {
    null
}