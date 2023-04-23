package com.mayakapps.kache

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A typealias that represents a function for calculating the size of a cache entry represented by the provided `key`
 * and `value`.
 *
 * For example, for [String][String], you can use:
 * ```
 * { _, text -> text.length }
 * ```
 *
 * If the entries have the same size or their size can't be determined, you can just return 1.
 */
typealias SizeCalculator<K, V> = (key: K, value: V) -> Long

/**
 * A typealias that represents a listener that is triggered when a cache entry is removed.
 *
 *
 * This is triggered when the entry represented by the `key` and `oldValue` is removed for any reason. If the removal
 * was a result of reaching the max size of the cache, `evicted` is true, otherwise its value is false. If the entry
 * was removed as a result of replacing it by one of the put operations, the new value is passed as `newValue`,
 * otherwise, `newValue` is null.
 */
typealias EntryRemovedListener<K, V> = (evicted: Boolean, key: K, oldValue: V, newValue: V?) -> Unit

/**
 * An in-memory Least Recently Used (LRU) cache.
 *
 * An LRU cache is a cache that holds strong references to a limited number of values. Each time a value is accessed,
 * it is moved to the head of a queue. When a value is added to a full cache, the value at the end of that queue is
 * evicted and may become eligible for garbage collection.
 *
 * @param maxSize The max size of this cache. For more information. See [InMemoryKache.maxSize].
 * @param sizeCalculator function used for calculating the size of the elements. See [SizeCalculator]
 * @param onEntryRemoved listener called when an entry is removed for any reason. See [EntryRemovedListener]
 * @param creationScope The coroutine scope used for executing `creationFunction` of put requests.
 */
class InMemoryKache<K : Any, V : Any>(
    maxSize: Long,
    strategy: KacheStrategy = KacheStrategy.LRU,
    private val creationScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val sizeCalculator: SizeCalculator<K, V> = { _, _ -> 1 },
    private val onEntryRemoved: EntryRemovedListener<K, V> = { _, _, _, _ -> },
) {
    init {
        require(maxSize > 0) { "maxSize must be positive value" }
    }

    private val creationMap = ConcurrentMutableMap<K, Deferred<V?>>()
    private val creationMutex = Mutex()

    private val map: MutableMap<K, V> = strategy.createMap()
    private val mapMutex = Mutex()

    /**
     * The max size of this cache in units calculated by [sizeCalculator]. This represents the max number of entries
     * if [sizeCalculator] used the default implementation (returning 1 for each entry),
     */
    var maxSize = maxSize
        private set

    /**
     * The current size of this cache in units calculated by [sizeCalculator]. This represents the current number of
     * entries if [sizeCalculator] used the default implementation (returning 1 for each entry),
     */
    var size = 0L
        private set

    suspend fun getKeys(): Set<K> = mapMutex.withLock { map.keys.toSet() }

    suspend fun getUnderCreationKeys(): Set<K> = mapMutex.withLock { creationMap.keys.toSet() }

    suspend fun getAllKeys(): Keys<K> =
        mapMutex.withLock { Keys(map.keys.toSet(), creationMap.keys.toSet()) }

    /**
     * Returns the value for [key] if it exists in the cache or wait for its creation if it is currently in progress.
     * This returns [defaultValue] if a value is not cached and wasn't in creation or cannot be created.
     *
     * It may even throw exceptions for unhandled exceptions in the currently in-progress creation block.
     */
    suspend fun getOrDefault(key: K, defaultValue: V): V =
        getFromCreation(key) ?: getIfAvailableOrDefault(key, defaultValue)

    /**
     * Returns the value for [key] if it exists in the cache or wait for its creation if it is currently in progress.
     * This returns `null` if a value is not cached and wasn't in creation or cannot be created.
     *
     * It may even throw exceptions for unhandled exceptions in the currently in-progress creation block.
     */
    suspend fun get(key: K): V? =
        getFromCreation(key) ?: getIfAvailable(key)

    /**
     * Returns the value for [key] if it already exists in the cache or [defaultValue] if it doesn't exist or creation
     * is still in progress.
     */
    fun getIfAvailableOrDefault(key: K, defaultValue: V): V =
        getIfAvailable(key) ?: defaultValue

    /**
     * Returns the value for [key] if it already exists in the cache or `null` if it doesn't exist or creation is still
     * in progress.
     */
    fun getIfAvailable(key: K): V? =
        map[key]


    /**
     * Returns the value for [key] if it exists in the cache, its creation is in progress or can be created by
     * [creationFunction]. If a value was returned, it is moved to the head of the queue. This returns `null` if a
     * value is not cached and cannot be created. You can imply that the creation has failed by returning `null`.
     * Any unhandled exceptions inside [creationFunction] won't be handled.
     */
    suspend fun getOrPut(key: K, creationFunction: suspend (key: K) -> V?): V? {
        get(key)?.let { return it }

        creationMutex.withLock {
            if (creationMap[key] == null && map[key] == null) {
                @Suppress("DeferredResultUnused")
                internalPutAsync(key, creationFunction)
            }
        }

        return get(key)
    }

    /**
     * Creates a new entry for [key] using [creationFunction] and returns the new value. Any existing value or
     * in-progress creation of [key] would be replaced by the new function. If a value was created, it is moved to the
     * head of the queue. This returns `null` if the value cannot be created. You can imply that the creation has
     * failed by returning `null`. Any unhandled exceptions inside [creationFunction] won't be handled.
     */
    suspend fun put(key: K, creationFunction: suspend (key: K) -> V?): V? =
        getFromCreation(key, putAsync(key, creationFunction))

    /**
     * Creates a new entry for [key] using [creationFunction] and returns a [Deferred]. Any existing value or
     * in-progress creation of [key] would be replaced by the new function. If a value was created, it is moved to the
     * head of the queue. You can imply that the creation has failed by returning `null`.
     */
    suspend fun putAsync(key: K, creationFunction: suspend (key: K) -> V?): Deferred<V?> =
        creationMutex.withLock { internalPutAsync(key, creationFunction) }

    private suspend fun internalPutAsync(
        key: K,
        mappingFunction: suspend (key: K) -> V?,
    ): Deferred<V?> {
        val deferred = creationScope.async {
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

                    oldValue?.let { onEntryRemoved(false, key, it, value) }
                }
            }

            value
        }

        deferred.invokeOnCompletion {
            @Suppress("DeferredResultUnused")
            creationMap.remove(key)
        }

        removeCreation(key, CODE_CREATION)
        creationMap[key] = deferred
        return deferred
    }

    /**
     * Caches [value] for [key]. The value is moved to the head of the queue. If there is a previous value or
     * in-progress creation, it will be removed/cancelled. It returns the previous value if it already exists,
     * or `null`
     */
    suspend fun put(key: K, value: V): V? {
        val oldValue = mapMutex.withLock {
            val oldValue = map.put(key, value)

            size += safeSizeOf(key, value) - (oldValue?.let { safeSizeOf(key, it) } ?: 0)
            removeCreation(key, CODE_VALUE)

            oldValue
        }

        trimToSize(maxSize)

        oldValue?.let { onEntryRemoved(false, key, it, value) }
        return oldValue
    }

    /**
     * Removes the entry and in-progress creation for [key] if it exists. It returns the previous value for [key].
     */
    suspend fun remove(key: K): V? {
        removeCreation(key)

        return mapMutex.withLock {
            val oldValue = map.remove(key)
            if (oldValue != null) size -= safeSizeOf(key, oldValue)
            oldValue
        }?.let { oldValue ->
            onEntryRemoved(false, key, oldValue, null)
            oldValue
        }
    }

    /**
     * Clears the cache, calling [onEntryRemoved] on each removed entry.
     */
    suspend fun clear() {
        for (key in creationMap.keys) {
            removeCreation(key)
        }

        mapMutex.withLock {
            with(map.iterator()) {
                forEach { (key, value) ->
                    remove()
                    onEntryRemoved(false, key, value, null)
                }
            }
        }
    }

    suspend fun removeAllUnderCreation() {
        mapMutex.withLock {
            for (key in creationMap.keys) {
                removeCreation(key)
            }
        }
    }

    /**
     * Sets the max size of the cache to [maxSize]. If the new maxSize is smaller than the previous value, the cache
     * would be trimmed.
     */
    suspend fun resize(maxSize: Long) {
        require(maxSize > 0) { "maxSize <= 0" }
        this.maxSize = maxSize
        trimToSize(maxSize)
    }

    /**
     * Remove the eldest entries until the total of remaining entries is/at/or below [size]. It won't affect the max
     * size of the cache, allowing it to grow again.
     */
    suspend fun trimToSize(size: Long) {
        mapMutex.withLock {
            nonLockedTrimToSize(size)
        }
    }

    private fun nonLockedTrimToSize(size: Long) {
        with(map.iterator()) {
            forEach { (key, value) ->
                if (this@InMemoryKache.size <= size) return@forEach
                remove()
                this@InMemoryKache.size -= safeSizeOf(key, value)
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

    private fun removeCreation(key: K, replacedWith: Int? = null) {
        val deferred = creationMap.remove(key)
        deferred?.cancel(
            message = CANCELLATION_MESSAGE,
            cause = replacedWith?.let { DeferredReplacedException(it) },
        )
    }

    data class Keys<K>(val keys: Set<K>, val underCreationKeys: Set<K>)
}

private const val CODE_CREATION = 1
private const val CODE_VALUE = 2

private class DeferredReplacedException(val replacedWith: Int) : CancellationException(CANCELLATION_MESSAGE)

private const val CANCELLATION_MESSAGE = "The cached element was removed before creation"