/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is copied from the AndroidX library with minor modifications to keep support for targets not supported by
 * AndroidX. It should be removed when AndroidX supports all targets.
 *
 * The original file can be found here:
 * https://android.googlesource.com/platform/frameworks/support/+/androidx-main/annotation/annotation/src/commonMain/kotlin/androidx/annotation/IntRange.kt
 *
 * Modifications:
 * - Rename package to `com.mayakapps.kache.annotation`
 * - Change visibility to internal.
 *
 * The file is up-to-date as of commit 34f5de5 on Jun 20, 2023.
 */

package com.mayakapps.kache.annotation

/**
 * Denotes that the annotated element should be an int or long in the given range.
 *
 * Example:
 * ```
 * @IntRange(from=0,to=255)
 * public int getAlpha() {
 *     ...
 * }
 * ```
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.ANNOTATION_CLASS
)
internal annotation class IntRange(
    /** Smallest value, inclusive */
    val from: Long = Long.MIN_VALUE,
    /** Largest value, inclusive */
    val to: Long = Long.MAX_VALUE
)
