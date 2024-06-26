/*
 * Copyright 2023-2024 MayakaApps
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
