package com.mayakapps.kache

import kotlinx.coroutines.Deferred

/**
 * An interface for a container cache. A container cache is a cache that stores a value for each key in a container i.e.
 * a file. It is used to cache files that are too large to be stored in memory. Storing an object in a container cache
 * requires serialization and deserialization logic that is handled by the user.
 *
 * @param K the type of keys in the cache.
 * @param C the type of containers in the cache.
 */
interface ContainerKache<K : Any, C : Any> {
    /**
     * Returns the container for [key] if it exists in the cache or waits for its creation if it is currently in
     * progress. This returns `null` if a file is not cached and isn't in creation or cannot be created. It may even
     * throw exceptions for unhandled exceptions in the currently in-progress creation block.
     */
    suspend fun get(key: String): C?

    /**
     * Returns the container for [key] if it already exists in the cache or `null` if it doesn't exist or creation is
     * still in progress.
     */
    suspend fun getIfAvailable(key: String): C?

    /**
     * Returns the container for [key] if it exists in the cache, its creation is in progress or can be created by
     * [creationFunction]. This returns `null` if a container is not cached and cannot be created. You can imply that
     * the creation has failed by returning `false`. Any unhandled exceptions inside [creationFunction] won't be handled.
     */
    suspend fun getOrPut(key: String, creationFunction: suspend (C) -> Boolean): C?

    /**
     * Creates a new container for [key] using [creationFunction] and returns the new value. Any existing container or
     * in-progress creation of [key] would be replaced by the new function. This returns `null` if the container cannot
     * be created. You can imply that the creation has failed by returning `false`. Any unhandled exceptions inside
     * [creationFunction] won't be handled.
     */
    suspend fun put(key: String, creationFunction: suspend (C) -> Boolean): C?

    /**
     * Creates a new container for [key] using [creationFunction] and returns a [Deferred]. Any existing container or
     * in-progress creation of [key] would be replaced by the new function. You can imply that the creation has failed
     * by returning `null`.
     */
    suspend fun putAsync(key: String, creationFunction: suspend (C) -> Boolean): Deferred<C?>

    /**
     * Removes the entry and in-progress creation for [key] if it exists. It returns the previous value for [key].
     */
    suspend fun remove(key: String)

    /**
     * Clears the cache.
     */
    suspend fun clear()

    /**
     * Closes the journal and cancels any in-progress creation.
     */
    suspend fun close()
}