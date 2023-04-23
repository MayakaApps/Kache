package com.mayakapps.kache

import kotlinx.coroutines.Deferred

interface ContainerKache<K : Any, C : Any> {
    /**
     * Returns the file for [key] if it exists in the cache or wait for its creation if it is currently in progress.
     * This returns `null` if a file is not cached and isn't in creation or cannot be created.
     *
     * It may even throw exceptions for unhandled exceptions in the currently in-progress creation block.
     */
    suspend fun get(key: String): C?

    /**
     * Returns the file for [key] if it already exists in the cache or `null` if it doesn't exist or creation is still
     * in progress.
     */
    suspend fun getIfAvailable(key: String): C?

    /**
     * Returns the file for [key] if it exists in the cache, its creation is in progress or can be created by
     * [creationFunction]. If a file is returned, it'll be moved to the head of the queue. This returns `null` if a
     * file is not cached and cannot be created. You can imply that the creation has failed by returning `false`.
     * Any unhandled exceptions inside [creationFunction] won't be handled.
     */
    suspend fun getOrPut(key: String, writeFunction: suspend (C) -> Boolean): C?

    /**
     * Creates a new file for [key] using [creationFunction] and returns the new value. Any existing file or
     * in-progress creation of [key] would be replaced by the new function. If a file is created, it'll be moved to the
     * head of the queue. This returns `null` if the file cannot be created. You can imply that the creation has
     * failed by returning `false`. Any unhandled exceptions inside [creationFunction] won't be handled.
     */
    suspend fun put(key: String, writeFunction: suspend (C) -> Boolean): C?

    /**
     * Creates a new file for [key] using [creationFunction] and returns a [Deferred]. Any existing file or
     * in-progress creation of [key] would be replaced by the new function. If a file is created, it'll be moved to the
     * head of the queue. You can imply that the creation has failed by returning `null`.
     */
    suspend fun putAsync(key: String, writeFunction: suspend (C) -> Boolean): Deferred<C?>

    /**
     * Removes the entry and in-progress creation for [key] if it exists. It returns the previous value for [key].
     */
    suspend fun remove(key: String)

    /**
     * Clears the cache, calling [onEntryRemoved] on each removed entry.
     */
    suspend fun clear()

    /**
     * Closes the journal file and cancels any in-progress creation.
     */
    suspend fun close()
}