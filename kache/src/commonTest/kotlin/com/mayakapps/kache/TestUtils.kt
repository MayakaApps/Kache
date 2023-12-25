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

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

internal fun runBasicInMemoryKacheRemoveListenerTest(
    maxSize: Long = MAX_SIZE,
    strategy: KacheStrategy = KacheStrategy.LRU,
    sizeCalculator: SizeCalculator<String, Int> = { _, _ -> 1 },
    testBody: suspend InMemoryKache<String, Int>.(MutableList<RemovedEntry<String, Int>>) -> Unit,
) = runTest {
    val removedEntries = mutableListOf<RemovedEntry<String, Int>>()
    // Explicit type parameter is a workaround for https://youtrack.jetbrains.com/issue/KT-53109
    val cache = InMemoryKache<String, Int>(maxSize) {
        this.strategy = strategy
        this.creationScope = this@runTest
        this.sizeCalculator = sizeCalculator
        this.onEntryRemoved = { evicted, key, oldValue, newValue ->
            removedEntries += RemovedEntry(evicted, key, oldValue, newValue)
        }
    }

    cache.testBody(removedEntries)
}

internal data class RemovedEntry<K, V>(val evicted: Boolean, val key: K, val oldValue: V, val newValue: V?)

internal fun runBasicInMemoryKacheTest(
    maxSize: Long = MAX_SIZE,
    strategy: KacheStrategy = KacheStrategy.LRU,
    sizeCalculator: SizeCalculator<String, Int> = { _, _ -> 1 },
    testBody: suspend InMemoryKache<String, Int>.(TestScope) -> Unit,
) = runInMemoryKacheTest(maxSize, strategy, sizeCalculator, testBody = testBody)

internal inline fun <K : Any, V : Any> runInMemoryKacheTest(
    maxSize: Long = MAX_SIZE,
    strategy: KacheStrategy = KacheStrategy.LRU,
    noinline sizeCalculator: SizeCalculator<K, V> = { _, _ -> 1 },
    noinline onEntryRemoved: EntryRemovedListener<K, V> = { _, _, _, _ -> },
    crossinline testBody: suspend InMemoryKache<K, V>.(TestScope) -> Unit,
) = runTest {
    testBody(
        InMemoryKache(maxSize) {
            this.strategy = strategy
            this.creationScope = this@runTest
            this.sizeCalculator = sizeCalculator
            this.onEntryRemoved = onEntryRemoved
        },
        this,
    )
}

internal const val MAX_SIZE = 10L
internal const val KEY_1 = "key 1"
internal const val VAL_1 = 201
internal const val KEY_2 = "key 2"
internal const val VAL_2 = 202
internal const val KEY_3 = "key 3"
internal const val VAL_3 = 203
internal const val KEY_4 = "key 4"
internal const val VAL_4 = 204
