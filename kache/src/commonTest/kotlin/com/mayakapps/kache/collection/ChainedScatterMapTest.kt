/*
 * Copyright 2024 MayakaApps
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

package com.mayakapps.kache.collection

import kotlin.test.Test
import kotlin.test.assertEquals

class ChainedScatterMapTest {

    @Test
    fun retainElementsAfterInternalResize() {
        val map = MutableChainedScatterMap<String, Int>(
            // The least possible capacity is 7
            initialCapacity = 7,
            accessChain = MutableChain(),
        )

        repeat(8) {
            map["$it"] = it
        }

        assertEquals(8, map.size)
        repeat(8) {
            assertEquals(it, map["$it"])
        }
    }
}
