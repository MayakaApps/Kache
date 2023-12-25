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

/**
 * A snapshot of the keys in the cache.
 *
 * [keys] are the keys that are currently in the cache. While, [underCreationKeys] are the keys that are currently
 * under creation.
 */
public data class KacheKeys<K>(val keys: Set<K>, val underCreationKeys: Set<K>)
