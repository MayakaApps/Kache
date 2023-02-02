package com.mayakapps.lrucache

/**
 * Base interface that can be used to implement custom key transformers for [DiskLruCache].
 *
 * In most cases, you don't need to implement one yourself. You can use [SHA256KeyHasher] instead or `null` if your
 * keys are already safe for filenames.
 */
fun interface KeyTransformer {

    /**
     * Returns a new transformed version of [oldKey]. Please note that the new key must follow filename guidelines.
     * To be safe, just limit yourself to characters, numbers, and underscores when possible. For more information,
     * see [this Wikipedia page](https://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations).
     */
    // Suppress false-positive: https://youtrack.jetbrains.com/issue/KTIJ-7642
    @Suppress("FUN_INTERFACE_WITH_SUSPEND_FUNCTION")
    suspend fun transform(oldKey: String): String
}

/**
 * An object that implements [KeyTransformer] and transforms keys to an SHA-256 hash of them.
 *
 * The last 1000 hashed values are cached in memory. This is used as the default [KeyTransformer] for [DiskLruCache].
 */
expect object SHA256KeyHasher : KeyTransformer {

    /**
     * Returns an SHA-256 hash of [oldKey] which may be newly generated or previously cached.
     */
    override suspend fun transform(oldKey: String): String
}