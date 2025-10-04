package com.safehill.kclient.util.retry

import kotlin.time.Duration


sealed interface RetryState {
    data object Idle : RetryState
    data object Executing : RetryState
    data class WaitingToRetry(
        val attempt: Int,
        val error: Throwable,
        val nextRetryDelay: Duration
    ) : RetryState

    data class Retrying(val attempt: Int) : RetryState
    data object Success : RetryState
    data class Failed(val error: Throwable, val totalAttempts: Int) : RetryState
}