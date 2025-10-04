package com.safehill.kclient.util.retry

import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

sealed interface RetryPolicy {
    val maxAttempts: Int
    fun shouldRetry(error: Throwable, attempt: Int): Boolean
    fun getDelay(attempt: Int): Duration

    data object None : RetryPolicy {
        override val maxAttempts = 0
        override fun shouldRetry(error: Throwable, attempt: Int) = false
        override fun getDelay(attempt: Int) = Duration.ZERO
    }


    data class ExponentialBackoff(
        override val maxAttempts: Int = 5,
        val initialDelay: Duration = 500.milliseconds,
        val maxDelay: Duration = 30.seconds,
        val multiplier: Double = 2.0,
        val retryOn: (Throwable) -> Boolean = { true }
    ) : RetryPolicy {
        override fun shouldRetry(error: Throwable, attempt: Int) =
            attempt < maxAttempts && retryOn(error)

        override fun getDelay(attempt: Int): Duration {
            val delay = initialDelay * multiplier.pow(attempt - 1)
            return minOf(delay, maxDelay)
        }
    }

}
