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

/**
 * Base interface that can be used to implement custom key transformers for `FileKache` implementations.
 *
 * In most cases, you don't need to implement one yourself. You can use [SHA256KeyHasher] instead or `null` if your
 * keys are already safe for filenames.
 */
public fun interface KeyTransformer {

    /**
     * Returns a new transformed version of [oldKey]. Please note that the new key must follow filename guidelines.
     * To be safe, just limit yourself to characters, numbers, and underscores when possible. For more information,
     * see [this Wikipedia page](https://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations).
     */
    public suspend fun transform(oldKey: String): String
}
