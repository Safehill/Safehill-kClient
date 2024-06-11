package com.safehill.kclient.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

inline fun <T> runCatchingPreservingCancellationException(
    block: () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend inline fun <T> safeApiCall(crossinline invoke: suspend () -> T): Result<T> {
    return withContext(Dispatchers.IO) {
        runCatchingPreservingCancellationException {
            invoke()
        }
    }
}