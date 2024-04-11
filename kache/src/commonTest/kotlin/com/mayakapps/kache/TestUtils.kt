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

import kotlinx.coroutines.test.TestScope
import kotlin.time.AbstractLongTimeSource
import kotlin.time.DurationUnit

internal fun TestScope.testInMemoryKache(
    maxSize: Long = MAX_SIZE,
    entryRemovalLogger: EntryRemovalLogger<String, Int>? = null,
    configuration: InMemoryKache.Configuration<String, Int>.() -> Unit = {}
) = InMemoryKache(maxSize) {
    creationScope = this@testInMemoryKache
    if (entryRemovalLogger != null) {
        onEntryRemoved = entryRemovalLogger::onEntryRemoved
    }
    configuration()
}


/**
 * Puts 4 elements into the kache and gets 2 of them. This way the state of the kache is as follows:
 * - The least-recently-used element is [KEY_3] with [VAL_3]
 * - The most-recently-used element is [KEY_2] with [VAL_2]
 * - The first-in element is [KEY_1] with [VAL_1]
 * - The last-in element is [KEY_4] with [VAL_4]
 */
internal suspend fun InMemoryKache<String, Int>.putFourElementsWithAccess() {
    put(KEY_1, VAL_1)
    put(KEY_2, VAL_2)
    put(KEY_3, VAL_3)
    put(KEY_4, VAL_4)
    get(KEY_1)
    get(KEY_2)
}

internal class EntryRemovalLogger<K, V> {
    private val removedEntries = mutableListOf<Event<K, V>>()

    internal fun onEntryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V?) {
        removedEntries += Event(evicted, key, oldValue, newValue)
    }

    internal fun getAndClearEvents() = removedEntries.toList().also { removedEntries.clear() }

    internal fun clear() = removedEntries.clear()

    internal data class Event<K, V>(val evicted: Boolean, val key: K, val oldValue: V, val newValue: V?)
}

internal class MsTimeSource : AbstractLongTimeSource(unit = DurationUnit.MILLISECONDS) {
    private var reading = 0L

    override fun read(): Long = reading

    operator fun plusAssign(milliseconds: Long) {
        reading += milliseconds
    }
}

internal const val MAX_SIZE = 10L
internal const val KEY_1 = "one"
internal const val VAL_1 = 1
internal const val KEY_2 = "two"
internal const val VAL_2 = 2
internal const val KEY_3 = "three"
internal const val VAL_3 = 3
internal const val KEY_4 = "four"
internal const val VAL_4 = 4
internal const val KEY_5 = "five"
internal const val VAL_5 = 5
