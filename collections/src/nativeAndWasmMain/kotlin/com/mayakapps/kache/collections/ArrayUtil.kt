/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package com.mayakapps.kache.collections

/*
 * Copied from Kotlin 1.8.20 from:
 *   https://github.com/JetBrains/kotlin/blob/v1.8.20/kotlin-native/runtime/src/main/kotlin/generated/_ArraysNative.kt
 */

/**
 * Returns new array which is a copy of the original array's range between [fromIndex] (inclusive)
 * and [toIndex] (exclusive) with new elements filled with **lateinit** _uninitialized_ values.
 * Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
internal fun <T> Array<T>.copyOfUninitializedElements(fromIndex: Int, toIndex: Int): Array<T> {
    val newSize = toIndex - fromIndex
    if (newSize < 0) {
        throw IllegalArgumentException("$fromIndex > $toIndex")
    }
    val result = arrayOfUninitializedElements<T>(newSize)
    this.copyInto(result, 0, fromIndex, toIndex.coerceAtMost(size))
    return result
}

/*
 * Copied from Kotlin 1.8.20 from:
 *   https://github.com/JetBrains/kotlin/blob/v1.8.20/kotlin-native/runtime/src/main/kotlin/generated/_ArraysNative.kt
 */

/**
 * Returns new array which is a copy of the original array with new elements filled with **lateinit** _uninitialized_ values.
 * Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
internal fun <T> Array<T>.copyOfUninitializedElements(newSize: Int): Array<T> {
    return copyOfUninitializedElements(0, newSize)
}

/*
 * Copied from Kotlin 1.8.20 from:
 *   https://github.com/JetBrains/kotlin/blob/v1.8.20/kotlin-native/runtime/src/main/kotlin/kotlin/collections/ArrayUtil.kt
 */

/**
 * Returns an array of objects of the given type with the given [size], initialized with _uninitialized_ values.
 * Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
@PublishedApi
internal inline fun <E> arrayOfUninitializedElements(size: Int): Array<E> {
    // TODO: special case for size == 0?
    require(size >= 0) { "capacity must be non-negative." }
    @Suppress("TYPE_PARAMETER_AS_REIFIED")
    // MODIFICATION: replaced with arrayOfNulls
//    return Array<E>(size)
    return arrayOfNulls<E>(size) as Array<E>
}

/*
 * Copied from Kotlin 1.8.20 from:
 *   https://github.com/JetBrains/kotlin/blob/v1.8.20/kotlin-native/runtime/src/main/kotlin/kotlin/collections/ArrayUtil.kt
 */

/**
 * Resets an array element at a specified index to some implementation-specific _uninitialized_ value.
 * In particular, references stored in this element are released and become available for garbage collection.
 * Attempts to read _uninitialized_ value work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
internal fun <E> Array<E>.resetAt(index: Int) {
    (@Suppress("UNCHECKED_CAST")(this as Array<Any?>))[index] = null
}


/*
 * Copied from Kotlin 1.8.20 from:
 *   https://github.com/JetBrains/kotlin/blob/v1.8.20/kotlin-native/runtime/src/main/kotlin/kotlin/collections/ArrayUtil.kt
 */

/**
 * Resets a range of array elements at a specified [fromIndex] (inclusive) to [toIndex] (exclusive) range of indices
 * to some implementation-specific _uninitialized_ value.
 * In particular, references stored in these elements are released and become available for garbage collection.
 * Attempts to read _uninitialized_ values work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
internal fun <E> Array<E>.resetRange(fromIndex: Int, toIndex: Int) {
    // MODIFICATION: replaced with fill
//    arrayFill(@Suppress("UNCHECKED_CAST") (this as Array<Any?>), fromIndex, toIndex, null)
    @Suppress("UNCHECKED_CAST") (this as Array<Any?>).fill(null, fromIndex, toIndex)
}