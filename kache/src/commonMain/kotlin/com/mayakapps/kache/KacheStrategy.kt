package com.mayakapps.kache

/**
 * Specifies the strategy to use when the cache has reached its capacity. The most common strategy is
 * LRU (Least Recently Used).
 */
public enum class KacheStrategy {

    /**
     * Least Recently Used. Discards the least recently used items first.
     * See [Wikipedia](https://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)).
     */
    LRU,

    /**
     * Most Recently Used. Discards the most recently used items first.
     * See [Wikipedia](https://en.wikipedia.org/wiki/Cache_replacement_policies#Most_recently_used_(MRU)).
     */
    MRU,

    /**
     * First In First Out. Discards the oldest items first.
     * See [Wikipedia](https://en.wikipedia.org/wiki/Cache_replacement_policies#First_in_first_out_(FIFO)).
     */
    FIFO,

    /**
     * First In Last Out. Discards the newest items first.
     * See [Wikipedia](https://en.wikipedia.org/wiki/Cache_replacement_policies#First_in_first_out_(FIFO)).
     */
    FILO,
}
