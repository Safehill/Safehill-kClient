package com.safehill.kclient.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalContracts::class)
inline fun <T> runCatchingSafe(
    block: () -> T
): Result<T> {
    contract {
        callsInPlace(block, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        println("Error $e")
        Result.failure(e)
    }
}

suspend inline fun <T> safeApiCall(crossinline invoke: suspend () -> T): Result<T> {
    return withContext(Dispatchers.IO) {
        runCatchingSafe {
            invoke()
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun Throwable.isCancellationException(): Boolean {
    contract {
        returns(true) implies (this@isCancellationException is kotlinx.coroutines.CancellationException)
    }
    return (this is CancellationException)
}