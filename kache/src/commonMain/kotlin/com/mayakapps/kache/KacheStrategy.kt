/*
 * Copyright 2023 MayakaApps
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
