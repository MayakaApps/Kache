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
 * An interface for a container cache. A container cache is a cache that stores a value for each key in a container i.e.
 * a file. It is used to cache files that are too large to be stored in memory. Storing an object in a container cache
 * requires serialization and deserialization logic that is handled by the user.
 */
public interface ContainerKache<K : Any, C : Any> {

    /**
     * The max size of this cache in bytes. It doesn't include the size of the journal.
     */
    public val maxSize: Long

    /**
     * The current size of this cache in bytes. It doesn't include the size of the journal.
     */
    public val size: Long

    /**
     * Returns a set of the keys that are currently in the cache, not under-creation keys.
     */
    public suspend fun getKeys(): Set<K>

    /**
     * Returns a set of the keys that are currently under creation.
     */
    public suspend fun getUnderCreationKeys(): Set<K>

    /**
     * Returns a [KacheKeys] instance that represents the keys that are currently in the cache, along with those that
     * are under creation.
     */
    public suspend fun getAllKeys(): KacheKeys<K>

    /**
     * Returns the container for [key] if it exists in the cache or waits for its creation if it is currently in
     * progress. This returns `null` if a file is not cached and isn't in creation or cannot be created. It may even
     * throw exceptions for unhandled exceptions in the currently in-progress creation block.
     */
    public suspend fun get(key: String): C?

    /**
     * Returns the container for [key] if it already exists in the cache or `null` if it doesn't exist or creation is
     * still in progress.
     */
    public suspend fun getIfAvailable(key: String): C?

    /**
     * Returns the container for [key] if it exists in the cache, its creation is in progress or can be created by
     * [creationFunction]. This returns `null` if a container is not cached and cannot be created. You can imply that
     * the creation has failed by returning `false`. Any unhandled exceptions inside [creationFunction] won't be handled.
     */
    public suspend fun getOrPut(key: String, creationFunction: suspend (C) -> Boolean): C?

    /**
     * Creates a new container for [key] using [creationFunction] and returns the new value. Any existing container or
     * in-progress creation of [key] would be replaced by the new function. This returns `null` if the container cannot
     * be created. You can imply that the creation has failed by returning `false`. Any unhandled exceptions inside
     * [creationFunction] won't be handled.
     */
    public suspend fun put(key: String, creationFunction: suspend (C) -> Boolean): C?

    /**
     * Creates a new container for [key] using [creationFunction] and returns a [Deferred]. Any existing container or
     * in-progress creation of [key] would be replaced by the new function. You can imply that the creation has failed
     * by returning `null`.
     */
    public suspend fun putAsync(key: String, creationFunction: suspend (C) -> Boolean): Deferred<C?>

    /**
     * Removes the entry and in-progress creation for [key] if it exists. It returns the previous value for [key].
     */
    public suspend fun remove(key: String)

    /**
     * Clears the cache.
     */
    public suspend fun clear()

    /**
     * Cancels all in-progress creations.
     */
    public suspend fun removeAllUnderCreation()

    /**
     * Sets the max size of the cache to [maxSize]. If the new maxSize is smaller than the previous value, the cache
     * would be trimmed.
     */
    public suspend fun resize(maxSize: Long)

    /**
     * Remove entries according to the policy defined by strategy until the total of remaining entries is/at/or below
     * [size]. It won't affect the max size of the cache, allowing it to grow again.
     */
    public suspend fun trimToSize(size: Long)

    /**
     * Closes the journal and cancels any in-progress creation.
     */
    public suspend fun close()
}
