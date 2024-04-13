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
 * An interface that represents a cache that holds entries within containers such as files.
 *
 * The user is tasked with the serialization and deserialization of the container.
 *
 * This module does not provide an implementation for this interface. An implementation can be found in the `:file-kache` module.
 */
public interface ContainerKache<K : Any, C : Any> {

    /**
     * Returns the maximum capacity of this cache, defined by the implementation.
     *
     * This typically excludes the size of keys, metadata, or the journal, if any.
     */
    public val maxSize: Long

    /**
     * Returns the current size of the cache, defined by the implementation.
     *
     * This typically excludes the size of keys, metadata, or the journal, if any.
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
     * Returns the container corresponding to the given [key] if it exists or is currently being created, or `null` otherwise.
     *
     * The function waits for the creation to complete if it is in progress. If the creation fails, the function returns
     * `null`. Any unhandled exceptions inside the creation block will be thrown.
     */
    public suspend fun get(key: String): C?

    /**
     * Returns the container corresponding to the given [key], or `null` if such a key is not present in the cache.
     *
     * This function does not wait for the creation of the container if it is in progress, returning `null` instead.
     */
    public suspend fun getIfAvailable(key: String): C?

    /**
     * Returns the container corresponding to the given [key] if it exists or is currently being created, a new container
     * serialized by [creationFunction], or `null` if the creation fails.
     *
     * The function waits for the creation to complete if it is in progress. If the creation fails, the function returns
     * `null`. Any unhandled exceptions inside the creation block will be thrown. [creationFunction] is NOT used as a
     * fallback if the current creation fails.
     *
     * [creationFunction] should return `true` if the creation was successful, and `false` otherwise.
     */
    public suspend fun getOrPut(key: String, creationFunction: suspend (C) -> Boolean): C?

    /**
     * Associates a new container serialized by [creationFunction] with the given [key].
     *
     * This function waits for the creation to complete. If the creation fails, the function returns `null`. Any
     * unhandled exceptions inside the creation block will be thrown. Existing or under-creation containers associated
     * with [key] will be replaced by the new container.
     *
     * [creationFunction] should return `true` if the creation was successful, and `false` otherwise.
     */
    public suspend fun put(key: String, creationFunction: suspend (C) -> Boolean): C?

    /**
     * Associates a new container serialized by [creationFunction] asynchronously with the given [key].
     *
     * Any unhandled exceptions inside the creation block will be thrown. Existing or under-creation containers associated
     * with [key] will be replaced by the new container.
     *
     * [creationFunction] should return `true` if the creation was successful, and `false` otherwise.
     *
     * Returns: a [Deferred] that will complete with the new container if the creation was successful, or `null` otherwise.
     */
    public suspend fun putAsync(key: String, creationFunction: suspend (C) -> Boolean): Deferred<C?>

    /**
     * Removes the specified [key] and its corresponding container from the cache.
     *
     * If the container is under creation, the creation will be cancelled.
     */
    public suspend fun remove(key: String)

    /**
     * Removes all keys and their corresponding containers from the cache.
     */
    public suspend fun clear()

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
     * Closes the cache, releasing any resources associated with it.
     */
    public suspend fun close()
}
