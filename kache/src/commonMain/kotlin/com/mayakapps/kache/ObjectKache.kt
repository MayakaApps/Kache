package com.mayakapps.kache

import kotlinx.coroutines.Deferred

/**
 * An interface that represents a cache that stores objects by keys. It is the parent interface for [InMemoryKache].
 */
interface ObjectKache<K : Any, V : Any> {

    /**
     * The max size of this cache in units calculated by [SizeCalculator] of the cache. This represents the max number
     * of entries if [SizeCalculator] used the default implementation (returning 1 for each entry),
     */
    val maxSize: Long

    /**
     * The current size of this cache in units calculated by [SizeCalculator] of the cache. This represents the current
     * number of entries if [SizeCalculator] used the default implementation (returning 1 for each entry),
     */
    val size: Long

    /**
     * Returns a set of the keys that are currently in the cache, not under-creation keys.
     */
    suspend fun getKeys(): Set<K>

    /**
     * Returns a set of the keys that are currently under creation.
     */
    suspend fun getUnderCreationKeys(): Set<K>

    /**
     * Returns a [KacheKeys] instance that represents the keys that are currently in the cache, along with those that
     * are under creation.
     */
    suspend fun getAllKeys(): KacheKeys<K>

    /**
     * Returns the value for [key] if it exists in the cache or waits for its creation if it is currently in progress.
     * This returns [defaultValue] if a value is neither cached nor under creation or cannot be created.
     *
     * It may even throw exceptions for unhandled exceptions in the currently in-progress creation block.
     */
    suspend fun getOrDefault(key: K, defaultValue: V): V

    /**
     * Returns the value for [key] if it exists in the cache or waits for its creation if it is currently in progress.
     * This returns `null` if a value is neither cached nor under creation or cannot be created.
     *
     * It may even throw exceptions for unhandled exceptions in the currently in-progress creation block.
     */
    suspend fun get(key: K): V?

    /**
     * Returns the value for [key] if it exists in the cache or [defaultValue] if it doesn't exist or its creation
     * is still in progress.
     */
    fun getIfAvailableOrDefault(key: K, defaultValue: V): V

    /**
     * Returns the value for [key] if it exists in the cache or `null` if it doesn't exist or its creation is still
     * in progress.
     */
    fun getIfAvailable(key: K): V?

    /**
     * Returns the value for [key] if it exists in the cache, its creation is in progress, or it can be created by
     * [creationFunction]. This returns `null` if a value is not cached and cannot be created. You can imply that the
     * creation has failed by returning `null`. Any unhandled exceptions inside [creationFunction] won't be handled.
     */
    suspend fun getOrPut(key: K, creationFunction: suspend (key: K) -> V?): V?

    /**
     * Creates a new entry for [key] using [creationFunction] and returns the new value. Any existing value or
     * in-progress creation of [key] would be replaced by the new function. This returns `null` if the value cannot be
     * created. You can imply that the creation has failed by returning `null`. Any unhandled exceptions inside
     * [creationFunction] won't be handled.
     */
    suspend fun put(key: K, creationFunction: suspend (key: K) -> V?): V?

    /**
     * Creates a new entry for [key] using [creationFunction] and returns a [Deferred]. Any existing value or
     * in-progress creation of [key] would be replaced by the new function. You can imply that the creation has failed
     * by returning `null`.
     */
    suspend fun putAsync(key: K, creationFunction: suspend (key: K) -> V?): Deferred<V?>

    /**
     * Caches [value] for [key]. If there is a previous value or in-progress creation, it will be replaced/cancelled.
     * It returns the previous value if it already exists, or `null` if it doesn't exist or its creation is still in
     * progress.
     */
    suspend fun put(key: K, value: V): V?

    /**
     * Caches all entries in [from]. If there is a previous value or in-progress creation for any of the keys, it will
     * be replaced/cancelled.
     */
    suspend fun putAll(from: Map<out K, V>)

    /**
     * Removes the entry and in-progress creation for [key] if it exists. It returns the previous value for [key].
     */
    suspend fun remove(key: K): V?

    /**
     * Clears the cache, calling [EntryRemovedListener] on each removed entry with `evicted` set to `false`.
     */
    suspend fun clear()

    /**
     * Evicts all entries, calling [EntryRemovedListener] on each removed entry with `evicted` set to `true`.
     */
    suspend fun evictAll()

    /**
     * Cancels all in-progress creations.
     */
    suspend fun removeAllUnderCreation()

    /**
     * Sets the max size of the cache to [maxSize]. If the new maxSize is smaller than the previous value, the cache
     * would be trimmed.
     */
    suspend fun resize(maxSize: Long)

    /**
     * Remove the eldest entries until the total of remaining entries is/at/or below [size]. It won't affect the max
     * size of the cache, allowing it to grow again.
     */
    suspend fun trimToSize(size: Long)
}