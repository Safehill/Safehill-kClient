package com.safehill.kclient.tasks.upload

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Defines retry behavior for failed uploads
 */
interface RetryPolicy {
    suspend fun shouldRetry(attempt: Int, error: Throwable): Boolean
    suspend fun delayBeforeRetry(attempt: Int): Duration
}

/**
 * Exponential backoff retry policy with jitter
 */
class ExponentialBackoffRetryPolicy(
    private val maxAttempts: Int = 3,
    private val baseDelay: Duration = 2.seconds,
    private val maxDelay: Duration = 30.seconds,
    private val backoffMultiplier: Double = 2.0,
) : RetryPolicy {

    override suspend fun shouldRetry(attempt: Int, error: Throwable): Boolean {
        return when {
            attempt >= maxAttempts -> false
            isRetriableError(error) -> true
            else -> false
        }
    }

    override suspend fun delayBeforeRetry(attempt: Int): Duration {
        if (attempt == 1) return Duration.ZERO
        val exponentialDelay = baseDelay * backoffMultiplier.pow(attempt - 1)
        return minOf(exponentialDelay, maxDelay)
    }

    private fun isRetriableError(error: Throwable): Boolean {
        return UploadErrorHandler.isRetriableError(error)
    }

    private fun Double.pow(exponent: Int): Double {
        var result = 1.0
        repeat(exponent) { result *= this }
        return result
    }
}

/**
 * Immediate retry policy for testing or aggressive retry scenarios
 */
class ImmediateRetryPolicy(
    private val maxAttempts: Int = 1
) : RetryPolicy {

    override suspend fun shouldRetry(attempt: Int, error: Throwable): Boolean {
        return attempt < maxAttempts
    }

    override suspend fun delayBeforeRetry(attempt: Int): Duration {
        return Duration.ZERO
    }
}

/**
 * No retry policy
 */
object NoRetryPolicy : RetryPolicy {
    private val maxAttempts: Int = 1

    override suspend fun shouldRetry(attempt: Int, error: Throwable): Boolean = false

    override suspend fun delayBeforeRetry(attempt: Int): Duration = Duration.ZERO
}