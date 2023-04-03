package com.mayakapps.kache

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult

internal fun combineResults(
    failureMessage: String,
    negatedFailureMessage: String,
    vararg results: MatcherResult,
) = MatcherResult(
    results.all { it.passed() },
    {
        "$failureMessage - Details:\n" +
                results.mapNotNull { result ->
                    if (result.passed()) return@mapNotNull null
                    result.failureMessage()
                }.joinToString("\n").prependIndent("    ")
    },
    {
        "$negatedFailureMessage - Details:\n" +
                results.mapNotNull { result ->
                    if (!result.passed()) return@mapNotNull null
                    result.negatedFailureMessage()
                }.joinToString("\n").prependIndent("    ")
    },
)

internal fun <T> T.named(name: String) = genericMatcher(name, this)

private fun <T> genericMatcher(name: String, expected: T) = Matcher<T> { value ->
    MatcherResult(
        passed = value == expected,
        failureMessageFn = { "$name ($value) should be ($expected)" },
        negatedFailureMessageFn = { "$name ($value) should not be ($expected)" },
    )
}