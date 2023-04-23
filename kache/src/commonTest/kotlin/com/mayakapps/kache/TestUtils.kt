package com.mayakapps.kache

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
internal fun runBasicInMemoryKacheRemoveListenerTest(
    maxSize: Long = MAX_SIZE,
    strategy: KacheStrategy = KacheStrategy.LRU,
    sizeCalculator: SizeCalculator<String, Int> = { _, _ -> 1 },
    testBody: suspend InMemoryKache<String, Int>.(MutableList<RemovedEntry<String, Int>>) -> Unit,
) = runTestSoftly {
    val removedEntries = mutableListOf<RemovedEntry<String, Int>>()
    val cache = InMemoryKache(
        maxSize, strategy, creationScope = this, sizeCalculator,
        onEntryRemoved = { evicted, key, oldValue, newValue ->
            removedEntries += RemovedEntry(evicted, key, oldValue, newValue)
        },
    )

    cache.testBody(removedEntries)
}

internal data class RemovedEntry<K, V>(val evicted: Boolean, val key: K, val oldValue: V, val newValue: V?)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runBasicInMemoryKacheTest(
    maxSize: Long = MAX_SIZE,
    strategy: KacheStrategy = KacheStrategy.LRU,
    sizeCalculator: SizeCalculator<String, Int> = { _, _ -> 1 },
    testBody: suspend InMemoryKache<String, Int>.(TestScope) -> Unit,
) = runInMemoryKacheTest(maxSize, strategy, sizeCalculator, testBody = testBody)

@OptIn(ExperimentalCoroutinesApi::class)
internal inline fun <K : Any, V : Any> runInMemoryKacheTest(
    maxSize: Long = MAX_SIZE,
    strategy: KacheStrategy = KacheStrategy.LRU,
    noinline sizeCalculator: SizeCalculator<K, V> = { _, _ -> 1 },
    noinline onEntryRemoved: EntryRemovedListener<K, V> = { _, _, _, _ -> },
    crossinline testBody: suspend InMemoryKache<K, V>.(TestScope) -> Unit,
) = runTestSoftly {
    testBody(
        InMemoryKache(
            maxSize, strategy, creationScope = this, sizeCalculator, onEntryRemoved,
        ),
        this,
    )
}

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
internal const val KEY_1 = "key 1"
internal const val VAL_1 = 201
internal const val KEY_2 = "key 2"
internal const val VAL_2 = 202
internal const val KEY_3 = "key 3"
internal const val VAL_3 = 203
internal const val KEY_4 = "key 4"
internal const val VAL_4 = 204