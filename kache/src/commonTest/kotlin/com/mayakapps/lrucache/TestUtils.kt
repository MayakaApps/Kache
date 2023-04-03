package com.mayakapps.lrucache

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runBasicLruCacheRemoveListenerTest(
    maxSize: Long = MAX_SIZE,
    sizeCalculator: SizeCalculator<String, Int> = { _, _ -> 1 },
    testBody: suspend LruCache<String, Int>.(MutableList<RemovedEntry<String, Int>>) -> Unit,
) = runTestSoftly {
    val removedEntries = mutableListOf<RemovedEntry<String, Int>>()
    val cache = LruCache(
        maxSize, creationScope = this, sizeCalculator,
        onEntryRemoved = { evicted, key, oldValue, newValue ->
            removedEntries += RemovedEntry(evicted, key, oldValue, newValue)
        },
    )

    cache.testBody(removedEntries)
}

internal data class RemovedEntry<K, V>(val evicted: Boolean, val key: K, val oldValue: V, val newValue: V?)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runBasicLruCacheTest(
    maxSize: Long = MAX_SIZE,
    sizeCalculator: SizeCalculator<String, Int> = { _, _ -> 1 },
    testBody: suspend LruCache<String, Int>.() -> Unit,
) = runLruCacheTest(maxSize, sizeCalculator, testBody = testBody)

@OptIn(ExperimentalCoroutinesApi::class)
internal inline fun <K : Any, V : Any> runLruCacheTest(
    maxSize: Long = MAX_SIZE,
    noinline sizeCalculator: SizeCalculator<K, V> = { _, _ -> 1 },
    noinline onEntryRemoved: EntryRemovedListener<K, V> = { _, _, _, _ -> },
    crossinline testBody: suspend LruCache<K, V>.() -> Unit,
) = runTestSoftly { testBody(LruCache(maxSize, creationScope = this, sizeCalculator, onEntryRemoved)) }

@OptIn(ExperimentalCoroutinesApi::class)
internal inline fun runTestSoftly(
    context: CoroutineContext = EmptyCoroutineContext,
    dispatchTimeoutMs: Long = 60_000L,
    crossinline testBody: suspend TestScope.() -> Unit,
) = runTest(context, dispatchTimeoutMs) { assertSoftly { withTimeout(100L) { testBody() } } }

internal fun <T> T.asOldValue() = named("Old value")
internal fun <T> T.asPutResult() = named("Put result")
internal fun <T> T.asValue() = named("Value")
internal fun Boolean.asEvicted() = named("Evicted")
internal fun <T> T.asKey() = named("Key")
internal fun Number.asSize() = toLong().named("Size")
internal fun Number.asMaxSize() = toLong().named("Max size")
internal fun <T> T.named(name: String) = genericMatcher(name, this)

private fun <T> genericMatcher(name: String, expected: T) = Matcher<T> { value ->
    MatcherResult(
        passed = value == expected,
        failureMessageFn = { "$name ($value) should be ($expected)" },
        negatedFailureMessageFn = { "$name ($value) should not be ($expected)" },
    )
}

internal const val MAX_SIZE = 10L
internal const val KEY = "key"
internal const val ALT_KEY = "alt_key"
internal const val VAL = 201
internal const val ALT_VAL = 202