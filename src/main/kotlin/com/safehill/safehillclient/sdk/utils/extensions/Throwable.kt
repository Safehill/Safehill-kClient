package com.safehill.safehillclient.sdk.utils.extensions

internal val Throwable.errorMsg: String
    get() = this.message ?: "Something went wrong."