package com.safehill.kclient.util.retry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RetryManager(
    private val policy: RetryPolicy = RetryPolicy.ExponentialBackoff()
) {
    private val _retryState = MutableStateFlow<RetryState>(RetryState.Idle)
    val retryState: StateFlow<RetryState> = _retryState.asStateFlow()

    suspend fun <T> execute(block: suspend () -> T): Result<T> =
        executeResult { runCatching { block() } }

    suspend fun <T> executeResult(block: suspend () -> Result<T>): Result<T> {
        var lastError: Throwable? = null
        var attempt = 0
        _retryState.update { RetryState.Executing }
        var breakLoop = false
        while (attempt < policy.maxAttempts && !breakLoop) {
            if (attempt > 0) {
                _retryState.update { RetryState.Retrying(attempt + 1) }
            }

            block()
                .onSuccess {
                    _retryState.update { RetryState.Success }
                    return Result.success(it)
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    lastError = error
                    if (!policy.shouldRetry(error, attempt + 1)) {
                        breakLoop = true
                    } else {
                        val delayDuration = policy.getDelay(attempt + 1)
                        _retryState.update {
                            RetryState.WaitingToRetry(
                                attempt + 1,
                                error,
                                delayDuration
                            )
                        }
                        delay(delayDuration)
                    }
                }
            attempt++
        }
        val error = lastError!!
        _retryState.update { RetryState.Failed(error, attempt) }
        return Result.failure(error)
    }
}
