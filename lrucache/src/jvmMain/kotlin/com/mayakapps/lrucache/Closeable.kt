package com.mayakapps.lrucache

import java.io.Closeable
import kotlin.io.use

// Workaround for a bug: https://youtrack.jetbrains.com/issue/KT-37316
@Suppress("ACTUAL_WITHOUT_EXPECT")
internal actual typealias Closeable = Closeable

internal actual inline fun <T : Closeable?, R> T.use(block: (T) -> R): R = use(block)