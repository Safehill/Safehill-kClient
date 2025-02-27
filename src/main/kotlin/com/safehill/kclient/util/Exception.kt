package com.safehill.kclient.util

import com.safehill.kclient.network.exceptions.SafehillError
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun Throwable.isSafehillError(): Boolean {
    contract {
        returns(true) implies (this@isSafehillError is SafehillError)
    }
    return this is SafehillError
}

fun Throwable.isSafehillHttpConflict(): Boolean {
    return this.isSafehillError() && this is SafehillError.ClientError.Conflict
}

@OptIn(ExperimentalContracts::class)
fun Throwable.isSafehillHttpNotFound(): Boolean {
    contract {
        returns(true) implies (this@isSafehillHttpNotFound is SafehillError.ClientError.NotFound)
    }
    return this.isSafehillError() && this@isSafehillHttpNotFound is SafehillError.ClientError.NotFound
}