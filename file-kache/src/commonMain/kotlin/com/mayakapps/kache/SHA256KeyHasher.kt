/*
 * Copyright 2023 MayakaApps
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

import okio.ByteString.Companion.encodeUtf8

/**
 * An object that implements [KeyTransformer] and transforms keys to an SHA-256 hash of them.
 *
 * The last 1000 hashed values are cached in memory. This is used as the default [KeyTransformer] for any `FileKache`.
 */
public object SHA256KeyHasher : KeyTransformer {
    private val hashedCache = InMemoryKache<String, String>(maxSize = 1000)

    /**
     * Returns an SHA-256 hash of [oldKey] which may be newly generated or previously cached.
     */
    override suspend fun transform(oldKey: String): String = hashedCache.getOrPut(oldKey) {
        oldKey.encodeUtf8().sha256().hex().uppercase()
    }!! // Since our creation function never returns null, we can use not-null assertion
}
