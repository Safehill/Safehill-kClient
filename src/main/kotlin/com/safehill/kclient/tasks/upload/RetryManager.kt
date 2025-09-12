package com.safehill.kclient.tasks.upload

import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

interface RetryManager {
    suspend fun <T> executeWithRetry(
        operation: suspend (attempt: Int) -> Result<T>
    ): Result<T>
}

class DefaultRetryManager(
    private val retryPolicy: RetryPolicy,
    private val errorHandler: UploadErrorHandler = UploadErrorHandler
) : RetryManager {

    override suspend fun <T> executeWithRetry(
        operation: suspend (Int) -> Result<T>
    ): Result<T> {
        var attempt = 1
        while (true) {
            coroutineContext.ensureActive()
            val result = operation(attempt)
            result.onSuccess { return Result.success(it) }

            val error = result.exceptionOrNull()!!
            if (!retryPolicy.shouldRetry(attempt, error)) {
                return Result.failure(error)
            }

            attempt++
            delay(retryPolicy.delayBeforeRetry(attempt))
        }
    }

}