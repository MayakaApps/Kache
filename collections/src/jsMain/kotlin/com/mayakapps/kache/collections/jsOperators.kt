/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/*
 * A copy of jsOperators.kt from Kotlin 1.8.20 from:
 *   https://github.com/JetBrains/kotlin/blob/v1.8.20/libraries/stdlib/js-v1/src/kotlin/jsOperators.kt
 * Changes are marked with "MODIFICATION" comments.
 */

@file:Suppress("UNUSED_PARAMETER")

// MODIFICATION: package renamed to com.mayakapps.kache.collections
package com.mayakapps.kache.collections

// MODIFICATION: comment out the following line
//@kotlin.internal.InlineOnly
internal inline fun jsDeleteProperty(obj: Any, property: Any) {
    js("delete obj[property]")
}

// MODIFICATION: comment out the following line
//@kotlin.internal.InlineOnly
internal inline fun jsBitwiseOr(lhs: Any?, rhs: Any?): Int =
    js("lhs | rhs").unsafeCast<Int>()