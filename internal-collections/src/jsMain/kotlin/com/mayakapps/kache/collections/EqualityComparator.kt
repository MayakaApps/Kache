/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * A copy of EqualityComparator.kt from Kotlin 1.8.20 from:
 *   https://github.com/JetBrains/kotlin/blob/v1.8.20/libraries/stdlib/js/src/kotlin/collections/EqualityComparator.kt
 * Changes are marked with "MODIFICATION" comments.
 */

// MODIFICATION: package renamed to com.mayakapps.kache.collections
package com.mayakapps.kache.collections

internal interface EqualityComparator {
    /**
     * Subclasses must override to return a value indicating
     * whether or not two keys or values are equal.
     */
    abstract fun equals(value1: Any?, value2: Any?): Boolean

    /**
     * Subclasses must override to return the hash code of a given key.
     */
    abstract fun getHashCode(value: Any?): Int


    object HashCode : EqualityComparator {
        override fun equals(value1: Any?, value2: Any?): Boolean = value1 == value2

        override fun getHashCode(value: Any?): Int = value?.hashCode() ?: 0
    }
}