package com.mayakapps.lrucache.io

// Workaround for a bug: https://youtrack.jetbrains.com/issue/KT-37316
@Suppress("ACTUAL_WITHOUT_EXPECT")
internal actual typealias IOException = java.io.IOException

// Workaround for a bug: https://youtrack.jetbrains.com/issue/KT-37316
@Suppress("ACTUAL_WITHOUT_EXPECT")
internal actual typealias FileNotFoundException = java.io.FileNotFoundException