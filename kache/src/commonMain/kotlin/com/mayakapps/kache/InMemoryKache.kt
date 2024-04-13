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

import com.mayakapps.kache.InMemoryKache.Configuration
import com.mayakapps.kache.collection.MutableChain
import com.mayakapps.kache.collection.MutableChainedScatterMap
import com.mayakapps.kache.collection.MutableTimedChain
import com.mayakapps.kache.collection.mutableScatterMapOf
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * A function for calculating the size of a cache entry represented by the provided `key` * and `value`.
 *
 * For example, for [String], you can use:
 * ```
 * { _, text -> text.length }
 * ```
 *
 * If the entries have the same size or their size can't be determined, you can just return 1.
 */
public typealias SizeCalculator<K, V> = (key: K, value: V) -> Long

/**
 * A listener that is triggered when a cache entry is removed.
 *
 * This is triggered when the entry represented by the `key` and `oldValue` is removed for any reason. If the removal
 * was a result of reaching the max size of the cache, `evicted` is true, otherwise its value is false. If the entry
 * was removed as a result of replacing it by one of the put operations, the new value is passed as `newValue`,
 * otherwise, `newValue` is null.
 */
public typealias EntryRemovedListener<K, V> = (evicted: Boolean, key: K, oldValue: V, newValue: V?) -> Unit

/**
 * An in-memory coroutine-safe versatile cache that stores objects by keys.
 *
 * It can be built using the following syntax:
 * ```
 * val cache = InMemoryKache<String, String>(maxSize = 100) {
 *     strategy = KacheStrategy.LRU
 *     // Other configuration
 * }
 * ```
 *
 * @see Configuration
 */
public class InMemoryKache<K : Any, V : Any> internal constructor(
    maxSize: Long,
    strategy: KacheStrategy,
    private val creationScope: CoroutineScope,
    private val sizeCalculator: SizeCalculator<K, V>,
    private val onEntryRemoved: EntryRemovedListener<K, V>,
    timeSource: TimeSource,
    private val expireAfterWriteDuration: Duration,
    private val expireAfterAccessDuration: Duration,
) : ObjectKache<K, V> {

    private val creationMap = mutableScatterMapOf<K, Deferred<V?>>()
    private val creationMutex = Mutex()

    private val accessChain = when {
        expireAfterAccessDuration != Duration.INFINITE -> MutableTimedChain(
            initialCapacity = 0,
            timeSource = timeSource,
        )

        strategy == KacheStrategy.LRU || strategy == KacheStrategy.MRU -> MutableChain(0)
        else -> null
    }

    private val insertionChain = when {
        expireAfterWriteDuration != Duration.INFINITE -> MutableTimedChain(
            initialCapacity = 0,
            timeSource = timeSource,
        )

        strategy == KacheStrategy.FIFO || strategy == KacheStrategy.FILO -> MutableChain(0)
        else -> null
    }

    private val map: MutableChainedScatterMap<K, V> = MutableChainedScatterMap(
        accessChain = accessChain,
        insertionChain = insertionChain,
        accessOrder = strategy == KacheStrategy.LRU || strategy == KacheStrategy.MRU,
    )
    private val mapMutex = Mutex()

    /**
     * Returns the maximum capacity of this cache, calculated by `sizeCalculator`.
     *
     * This does not include the size of keys or necessary metadata e.g., access time.
     */
    override var maxSize: Long = maxSize
        private set

    /**
     * Returns the current size of the cache, calculated by `sizeCalculator`.
     *
     * This does not include the size of keys or necessary metadata e.g., access time.
     */
    override var size: Long = 0L
        private set

    private val reversed = strategy == KacheStrategy.MRU || strategy == KacheStrategy.FILO

    private val keySet = map.getKeySet(reversed = reversed)

    override suspend fun getKeys(): Set<K> = mapMutex.withLock {
        nonLockedEvictExpired()
        keySet.toSet()
    }

    override suspend fun getUnderCreationKeys(): Set<K> = mapMutex.withLock {
        nonLockedEvictExpired()
        creationMap.keySet.toSet()
    }

    override suspend fun getAllKeys(): KacheKeys<K> = mapMutex.withLock {
        nonLockedEvictExpired()
        KacheKeys(keySet.toSet(), creationMap.keySet.toSet())
    }

    override suspend fun getOrDefault(key: K, defaultValue: V): V {
        evictExpired()
        return getFromCreation(key) ?: getIfAvailableOrDefault(key, defaultValue)
    }

    override suspend fun get(key: K): V? {
        evictExpired()
        return getFromCreation(key) ?: getIfAvailable(key)
    }

    override fun getIfAvailableOrDefault(key: K, defaultValue: V): V =
        getIfAvailable(key) ?: defaultValue

    override fun getIfAvailable(key: K): V? {
        val index = map.findKeyIndex(key)
        if (index < 0) return null

        if (expireAfterAccessDuration != Duration.INFINITE) {
            val timeMark = (accessChain as MutableTimedChain).getTimeMark(index)
            if (timeMark == null || timeMark.elapsedNow() >= expireAfterAccessDuration) return null
        }

        if (expireAfterWriteDuration != Duration.INFINITE) {
            val timeMark = (insertionChain as MutableTimedChain).getTimeMark(index)
            if (timeMark == null || timeMark.elapsedNow() >= expireAfterWriteDuration) return null
        }

        return map[key]
    }


    override suspend fun getOrPut(key: K, creationFunction: suspend (key: K) -> V?): V? {
        get(key)?.let { return it }

        creationMutex.withLock {
            if (creationMap[key] == null && map[key] == null) {
                @Suppress("DeferredResultUnused")
                internalPutAsync(key, creationFunction)
            }
        }

        return get(key)
    }

    override suspend fun put(key: K, creationFunction: suspend (key: K) -> V?): V? =
        getFromCreation(key, putAsync(key, creationFunction))

    override suspend fun putAsync(key: K, creationFunction: suspend (key: K) -> V?): Deferred<V?> =
        creationMutex.withLock { internalPutAsync(key, creationFunction) }

    private suspend fun internalPutAsync(
        key: K,
        mappingFunction: suspend (key: K) -> V?,
    ): Deferred<V?> {
        val deferred = creationScope.async {
            try {
                val value = try {
                    mappingFunction(key)
                } catch (cancellation: CancellationException) {
                    null
                }

                if (value != null) {
                    // All operations inside the lock to prevent cancellation before trimming or
                    // invoking listener
                    mapMutex.withLock {
                        val oldValue = map.put(key, value)

                        size += safeSizeOf(key, value) - (oldValue?.let { safeSizeOf(key, it) } ?: 0)
                        nonLockedTrimToSize(maxSize)
                        nonLockedEvictExpired()

                        oldValue?.let { onEntryRemoved(false, key, it, value) }
                    }
                }

                value
            } finally {
                @Suppress("DeferredResultUnused")
                creationMutex.withLock {
                    creationMap.remove(key)
                }
            }
        }

        removeCreation(key, CODE_CREATION)
        creationMap[key] = deferred
        return deferred
    }

    override suspend fun put(key: K, value: V): V? {
        val oldValue = mapMutex.withLock {
            val oldValue = map.put(key, value)

            size += safeSizeOf(key, value) - (oldValue?.let { safeSizeOf(key, it) } ?: 0)
            removeCreation(key, CODE_VALUE)

            oldValue
        }

        oldValue?.let { onEntryRemoved(false, key, it, value) }

        trimToSize(maxSize)

        return oldValue
    }

    override suspend fun putAll(from: Map<out K, V>) {
        val removedEntries = mutableMapOf<K, V>()
        mapMutex.withLock {
            for ((key, value) in from) {
                val oldValue = map.put(key, value)

                size += safeSizeOf(key, value) - (oldValue?.let { safeSizeOf(key, it) } ?: 0)
                removeCreation(key, CODE_VALUE)

                if (oldValue != null) removedEntries[key] = oldValue
            }
        }

        for ((key, oldValue) in removedEntries) {
            onEntryRemoved(false, key, oldValue, from[key])
        }

        trimToSize(maxSize)
    }

    override suspend fun remove(key: K): V? {
        creationMutex.withLock {
            removeCreation(key)
        }

        return mapMutex.withLock {
            val oldValue = map.remove(key)
            if (oldValue != null) size -= safeSizeOf(key, oldValue)

            nonLockedEvictExpired()

            oldValue
        }?.let { oldValue ->
            onEntryRemoved(false, key, oldValue, null)
            oldValue
        }
    }

    override suspend fun clear() {
        creationMutex.withLock {
            removeAllCreations()
        }

        mapMutex.withLock {
            map.removeAllWithCallback(reversed = reversed) { key, value ->
                size -= safeSizeOf(key, value)
                onEntryRemoved(false, key, value, null)
            }

            check(size == 0L) {
                "sizeCalculator is reporting inconsistent results!"
            }
        }
    }

    override suspend fun evictAll() {
        creationMutex.withLock {
            removeAllCreations()
        }

        mapMutex.withLock {
            map.removeAllWithCallback(reversed = reversed) { key, value ->
                size -= safeSizeOf(key, value)
                onEntryRemoved(true, key, value, null)
            }

            check(size == 0L) {
                "sizeCalculator is reporting inconsistent results!"
            }
        }
    }

    override suspend fun removeAllUnderCreation() {
        mapMutex.withLock {
            creationMutex.withLock {
                removeAllCreations()
            }
        }
    }

    override suspend fun resize(maxSize: Long) {
        require(maxSize > 0) { "maxSize <= 0" }
        this.maxSize = maxSize
        trimToSize(maxSize)
    }

    override suspend fun trimToSize(size: Long) {
        mapMutex.withLock {
            nonLockedEvictExpired()
            nonLockedTrimToSize(size)
        }
    }

    private fun nonLockedTrimToSize(size: Long) {
        if (this@InMemoryKache.size <= size) return

        map.removeAllWithCallback(
            reversed = reversed,
            stopRemoving = { _, _, _ -> this@InMemoryKache.size <= size },
        ) { key, value ->
            this@InMemoryKache.size -= safeSizeOf(key, value)
            onEntryRemoved(true, key, value, null)
        }

        check(this.size >= 0 || (map.isEmpty() && this.size != 0L)) {
            "sizeCalculator is reporting inconsistent results!"
        }
    }

    override suspend fun evictExpired() {
        mapMutex.withLock {
            nonLockedEvictExpired()
        }
    }

    private fun nonLockedEvictExpired() {
        (accessChain as? MutableTimedChain)?.let { chain ->
            map.removeAllWithCallback(accessOrder = true,
                stopRemoving = { _, _, index ->
                    chain.getTimeMark(index)!!.elapsedNow() < expireAfterAccessDuration
                }
            ) { key, value ->
                size -= safeSizeOf(key, value)
                onEntryRemoved(true, key, value, null)
            }
        }

        (insertionChain as? MutableTimedChain)?.let { chain ->
            map.removeAllWithCallback(accessOrder = false,
                stopRemoving = { _, _, index ->
                    chain.getTimeMark(index)!!.elapsedNow() < expireAfterWriteDuration
                }
            ) { key, value ->
                size -= safeSizeOf(key, value)
                onEntryRemoved(true, key, value, null)
            }
        }

        check(this.size >= 0 || (map.isEmpty() && this.size != 0L)) {
            "sizeCalculator is reporting inconsistent results!"
        }
    }

    private fun safeSizeOf(key: K, value: V): Long {
        val size = sizeCalculator(key, value)
        check(size >= 0) { "Negative size: $key = $value" }
        return size
    }

    private suspend fun getFromCreation(key: K): V? =
        creationMap[key]?.let { deferred -> getFromCreation(key, deferred) }

    private suspend fun getFromCreation(key: K, creation: Deferred<V?>): V? {
        return try {
            creation.await()
        } catch (ex: CancellationException) {
            val cause = ex.cause
            if (cause is DeferredReplacedException) {
                when (cause.replacedWith) {
                    CODE_CREATION -> getFromCreation(key)
                    CODE_VALUE -> getIfAvailable(key)
                    else -> null
                }
            } else null
        }
    }

    private fun removeAllCreations() {
        creationMap.forEachKey { key ->
            removeCreation(key)
        }
    }

    private fun removeCreation(key: K, replacedWith: Int? = null) {
        val deferred = creationMap.remove(key)
        deferred?.cancel(
            message = CANCELLATION_MESSAGE,
            cause = replacedWith?.let { DeferredReplacedException(it) },
        )
    }

    /**
     * Configuration for [InMemoryKache]. It is used as a receiver of [InMemoryKache] builder.
     */
    public class Configuration<K, V>(
        /**
         * The maximum capacity of the cache.
         *
         * @see InMemoryKache.maxSize
         */
        public var maxSize: Long,
    ) {

        /**
         * The strategy used for evicting elements.
         *
         * @see KacheStrategy
         */
        public var strategy: KacheStrategy = KacheStrategy.LRU

        /**
         * The coroutine scope used for executing `creationFunction` of put requests.
         */
        public var creationScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

        /**
         * A function used for calculating the size of the elements.
         *
         * @see SizeCalculator
         */
        public var sizeCalculator: SizeCalculator<K, V> = { _, _ -> 1 }

        /**
         * A listener called when an entry is removed.
         *
         * @see EntryRemovedListener
         */
        public var onEntryRemoved: EntryRemovedListener<K, V> = { _, _, _, _ -> }

        /**
         * The time source used for calculating the time marks of the elements. Only used if the
         * [expireAfterWriteDuration] or [expireAfterAccessDuration] is set.
         *
         * @see TimeSource
         */
        public var timeSource: TimeSource = TimeSource.Monotonic

        /**
         * The duration after which the elements are removed after they are written.
         *
         * @see Duration
         */
        public var expireAfterWriteDuration: Duration = Duration.INFINITE

        /**
         * The duration after which the elements are removed after they are accessed.
         *
         * @see Duration
         */
        public var expireAfterAccessDuration: Duration = Duration.INFINITE
    }
}

/**
 * Creates a new instance of [InMemoryKache] with the given [maxSize] and [configuration].
 *
 * If [maxSize] is set inside [configuration], it will override the value passed as parameter.
 *
 * @see InMemoryKache.maxSize
 * @see InMemoryKache.Configuration
 */
public fun <K : Any, V : Any> InMemoryKache(
    maxSize: Long,
    configuration: Configuration<K, V>.() -> Unit = {},
): InMemoryKache<K, V> {
    require(maxSize > 0) { "maxSize must be positive value" }

    val config = Configuration<K, V>(maxSize).apply(configuration)
    return InMemoryKache(
        config.maxSize,
        config.strategy,
        config.creationScope,
        config.sizeCalculator,
        config.onEntryRemoved,
        config.timeSource,
        config.expireAfterWriteDuration,
        config.expireAfterAccessDuration,
    )
}

private const val CODE_CREATION = 1
private const val CODE_VALUE = 2

private class DeferredReplacedException(val replacedWith: Int) : CancellationException(CANCELLATION_MESSAGE)

private const val CANCELLATION_MESSAGE = "The cached element was removed before creation"
