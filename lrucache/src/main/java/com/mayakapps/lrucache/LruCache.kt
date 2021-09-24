package com.mayakapps.lrucache

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

typealias SizeCalculator<K, V> = (key: K, value: V) -> Long

typealias EntryRemovedListener<K, V> = (evicted: Boolean, key: K, oldValue: V, newValue: V?) -> Unit

/**
 * @param maxSize for caches that do not override {@link #sizeOf}, this is
 *     the maximum number of entries in the cache. For all other caches,
 *     this is the maximum sum of the sizes of the entries in this cache.
 * @param sizeCalculator function used for calculating the size of the elements.
 *     (Default: Always returns 1 to depend on items count)
 * @param maxSize for caches that do not override {@link #sizeOf}, this is
 *     the maximum number of entries in the cache. For all other caches,
 *     this is the maximum sum of the sizes of the entries in this cache.
 * @param maxSize for caches that do not override {@link #sizeOf}, this is
 *     the maximum number of entries in the cache. For all other caches,
 *     this is the maximum sum of the sizes of the entries in this cache.
 */
class LruCache<K : Any, V : Any>(
    maxSize: Long,
    private val sizeCalculator: SizeCalculator<K, V> = { _, _ -> 1 },
    private val onEntryRemoved: EntryRemovedListener<K, V> = { _, _, _, _ -> },
    private val creationScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {
    init {
        require(maxSize > 0) { "maxSize must be positive value" }
    }

    private val creationMap = ConcurrentHashMap<K, Deferred<V?>>(0, 0.75F, 1)
    private val creationMutex = Mutex()

    private val map = LinkedHashMap<K, V>(0, 0.75F, true)
    private val mapMutex = Mutex()

    var maxSize = maxSize
        private set

    /** Size of this cache in units. Not necessarily the number of elements. */
    private var size = 0L

    /**
     * Sets the size of the cache.
     *
     * @param maxSize The new maximum size.
     */
    suspend fun resize(maxSize: Long) {
        require(maxSize > 0) { "maxSize <= 0" }
        this.maxSize = maxSize
        trimToSize(maxSize)
    }

    /**
     * Returns the value for {@code key} if it exists in the cache or wait for its
     * creation if it is currently in progress. This returns null if a value is not
     * cached and cannot be created. It may even throw exception for unhandled exceptions
     * in creation block
     */
    suspend fun getOrDefault(key: K, defaultValue: V): V =
        getFromCreation(key) ?: getIfAvailableOrDefault(key, defaultValue)

    /**
     * Returns the value for {@code key} if it exists in the cache or wait for its
     * creation if it is currently in progress. This returns null if a value is not
     * cached and cannot be created. It may even throw exception for unhandled exceptions
     * in creation block
     */
    suspend fun get(key: K): V? =
        getFromCreation(key) ?: getIfAvailable(key)


    /**
     * Returns the value for {@code key} if it exists in the cache or {@code defaultValue}
     */
    suspend fun getIfAvailableOrDefault(key: K, defaultValue: V): V =
        mapMutex.withLock { map[key] } ?: defaultValue

    /**
     * Returns the value for {@code key} if it exists in the cache or null
     */
    suspend fun getIfAvailable(key: K): V? =
        mapMutex.withLock { map[key] }


    /**
     * Returns the value for {@code key} if it exists in the cache or can be
     * created by {@code creationFunction}. If a value was returned, it is moved to the
     * head of the queue. This returns null if a value is not cached and cannot
     * be created.
     */
    suspend fun getOrPut(key: K, creationFunction: suspend (key: K) -> V?) =
        creationMutex.withLock {
            get(key) ?: getFromCreation(key, internalPutAsync(key, creationFunction))
        }

    /**
     * Caches the result of {@code creationFunction} for {@code key}. The value is moved to the
     * head of the queue.
     *
     * @return the result for the creation block or null if creation failed
     */
    suspend fun put(key: K, creationFunction: suspend (key: K) -> V?): V? =
        getFromCreation(key, putAsync(key, creationFunction))

    /**
     * Caches the result of {@code creationFunction} for {@code key}. The value is moved to the
     * head of the queue.
     *
     * @return the deferred for the creation block
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

        creationMap[key] = deferred
        return deferred
    }

    /**
     * Caches {@code value} for {@code key}. The value is moved to the head of the queue.
     *
     * @return the previous value mapped by {@code key}.
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
     * Remove the eldest entries until the total of remaining entries is at or
     * below the requested size.
     *
     * @param maxSize the maximum size of the cache before returning. May be -1
     *            to evict even 0-sized elements.
     */
    suspend fun trimToSize(maxSize: Long) {
        mapMutex.withLock {
            nonLockedTrimToSize(maxSize)
        }
    }

    private fun nonLockedTrimToSize(maxSize: Long) {
        with(map.iterator()) {
            forEach { (key, value) ->
                if (size <= maxSize) return@forEach
                remove()
                size -= safeSizeOf(key, value)
                onEntryRemoved(true, key, value, null)
            }
        }

        check(size >= 0 || (map.isEmpty() && size != 0L)) {
            "sizeCalculator is reporting inconsistent results!"
        }
    }

    /**
     * Removes the entry for {@code key} if it exists.
     *
     * @return the previous value mapped by {@code key}.
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
     * Clear the cache, calling {@link #entryRemoved} on each removed entry.
     */
    suspend fun evictAll() {
        for ((key, _) in creationMap) {
            removeCreation(key)
        }

        trimToSize(maxSize = -1) // -1 will evict 0-sized elements
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
            message = "The cached element was removed before creation",
            cause = replacedWith?.let { DeferredReplacedException(it) },
        )
    }
}

private const val CODE_CREATION = 1
private const val CODE_VALUE = 2

private class DeferredReplacedException(val replacedWith: Int) : CancellationException()