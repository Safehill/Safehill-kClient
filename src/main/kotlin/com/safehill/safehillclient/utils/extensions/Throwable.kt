package com.safehill.safehillclient.utils.extensions

internal val Throwable.errorMsg: String
    get() = this.message ?: "Something went wrong."