package com.safehill.safehillclient.utils.extensions


fun <T> T.toSuccess() = Result.success(this)

fun Throwable.toFailure() = Result.failure<Nothing>(this)


fun String.toFailure() = Exception(this).toFailure()


inline fun <T, R> Result<T>.ifSuccess(f: (T) -> Result<R>) = mapCatching {
    f(it).getOrThrow()
}

inline fun <T, R> Result<T>.flatMapCatching(transform: (T) -> Result<R>): Result<R> {
    return this.mapCatching {
        transform(it).getOrThrow()
    }
}

fun <T> Result<T>.toUnitResult() = this.map { }
