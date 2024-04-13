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

import kotlinx.coroutines.Deferred

/**
 * An interface that represents a cache that holds entries in memory without serialization.
 */
public interface ObjectKache<K : Any, V : Any> {

    /**
     * Returns the maximum capacity of this cache, defined by the implementation.
     *
     * This typically excludes the size of keys or metadata, if any.
     */
    public val maxSize: Long

    /**
     * Returns the current size of the cache, defined by the implementation.
     *
     * This typically excludes the size of keys or metadata, if any.
     */
    public val size: Long

    /**
     * Returns a read-only [Set] of keys currently in the cache, excluding under-creation keys.
     */
    public suspend fun getKeys(): Set<K>

    /**
     * Returns a read-only [Set] of keys currently under creation.
     */
    public suspend fun getUnderCreationKeys(): Set<K>

    /**
     * Returns a [KacheKeys] object containing all keys in the cache, including under-creation keys.
     */
    public suspend fun getAllKeys(): KacheKeys<K>

    /**
     * Returns the value corresponding to the given [key] if it exists or is currently being created, or [defaultValue]
     * otherwise.
     *
     * The function waits for the creation to complete if it is in progress. If the creation fails, the function returns
     * [defaultValue]. Any unhandled exceptions inside the creation block will be thrown.
     */
    public suspend fun getOrDefault(key: K, defaultValue: V): V

    /**
     * Returns the value corresponding to the given [key] if it exists or is currently being created, or `null` otherwise.
     *
     * The function waits for the creation to complete if it is in progress. If the creation fails, the function returns
     * `null`. Any unhandled exceptions inside the creation block will be thrown.
     */
    public suspend fun get(key: K): V?

    /**
     * Returns the value corresponding to the given [key], or [defaultValue] if such a key is not present in the cache.
     *
     * This function does not wait for the creation of the value if it is in progress, returning [defaultValue] instead.
     *
     * Note that this function neither returns nor removes the value if it is expired.
     */
    public fun getIfAvailableOrDefault(key: K, defaultValue: V): V

    /**
     * Returns the value corresponding to the given [key], or `null` if such a key is not present in the cache.
     *
     * This function does not wait for the creation of the value if it is in progress, returning `null` instead.
     *
     * Note that this function neither returns nor removes the value if it is expired.
     */
    public fun getIfAvailable(key: K): V?

    /**
     * Returns the value corresponding to the given [key] if it exists, is currently being created, a new value created by
     * [creationFunction], or `null` if the creation fails.
     *
     * The function waits for the creation to complete if it is in progress. If the creation fails, the function returns
     * `null`. Any unhandled exceptions inside the creation block will be thrown. [creationFunction] is NOT used as a
     * fallback if the current creation fails.
     *
     * [creationFunction] should return `null` only if the creation fails.
     */
    public suspend fun getOrPut(key: K, creationFunction: suspend (key: K) -> V?): V?

    /**
     * Associates a new value created by [creationFunction] with the given [key].
     *
     * This function waits for the creation to complete. If the creation fails, the function returns `null`. Any
     * unhandled exceptions inside the creation block will be thrown. Existing or under-creation values associated with
     * [key] will be replaced by the new value.
     *
     * [creationFunction] should return `null` only if the creation fails.
     */
    public suspend fun put(key: K, creationFunction: suspend (key: K) -> V?): V?

    /**
     * Associates a new value created by [creationFunction] asynchronously with the given [key].
     *
     * Any unhandled exceptions inside the creation block will be thrown. Existing or under-creation values associated
     * with [key] will be replaced by the new value.
     *
     * [creationFunction] should return `null` only if the creation fails.
     *
     * Returns: a [Deferred] that will complete with the new value if the creation was successful, or `null` otherwise.
     */
    public suspend fun putAsync(key: K, creationFunction: suspend (key: K) -> V?): Deferred<V?>

    /**
     * Associates the specified [value] with the specified [key] in the cache.
     *
     * Existing or under-creation values associated with [key] will be replaced by the new value.
     *
     * Returns: the previous value associated with [key], or `null` if there was no previous value.
     */
    public suspend fun put(key: K, value: V): V?

    /**
     * Updates this cache with key/value pairs from the specified map [from].
     *
     * Existing or under-creation values associated with keys in [from] will be replaced by the new values.
     */
    public suspend fun putAll(from: Map<out K, V>)

    /**
     * Removes the specified [key] and its corresponding value from the cache.
     *
     * If the value is under creation, the creation will be cancelled.
     */
    public suspend fun remove(key: K): V?

    /**
     * Clears the cache, calling [EntryRemovedListener] on each removed entry with `evicted` set to `false`.
     */
    public suspend fun clear()

    /**
     * Removes all keys and their corresponding values from the cache.
     *
     * [EntryRemovedListener] will be called for each removed entry with `evicted` set to `true`.
     */
    public suspend fun evictAll()

    /**
     * Cancels all in-progress creations.
     */
    public suspend fun removeAllUnderCreation()

    /**
     * Sets the maximum capacity of this cache to [maxSize].
     *
     * If the new maxSize is smaller than the previous value, the cache would be trimmed.
     */
    public suspend fun resize(maxSize: Long)

    /**
     * Remove entries from the cache until the size is less than or equal to [size].
     *
     * If the current size is already less than or equal to [size], this function does nothing. The capacity of the
     * cache is not changed.
     */
    public suspend fun trimToSize(size: Long)

    /**
     * Removes all expired keys and their corresponding values from the cache.
     *
     * [EntryRemovedListener] will be called for each removed entry with `evicted` set to `true`.
     */
    public suspend fun evictExpired()
}
