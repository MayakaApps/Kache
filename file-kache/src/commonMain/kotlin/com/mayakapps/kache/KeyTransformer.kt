package com.mayakapps.kache

/**
 * Base interface that can be used to implement custom key transformers for `FileKache` implementations.
 *
 * In most cases, you don't need to implement one yourself. You can use [SHA256KeyHasher] instead or `null` if your
 * keys are already safe for filenames.
 */
public fun interface KeyTransformer {

    /**
     * Returns a new transformed version of [oldKey]. Please note that the new key must follow filename guidelines.
     * To be safe, just limit yourself to characters, numbers, and underscores when possible. For more information,
     * see [this Wikipedia page](https://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations).
     */
    public suspend fun transform(oldKey: String): String
}
