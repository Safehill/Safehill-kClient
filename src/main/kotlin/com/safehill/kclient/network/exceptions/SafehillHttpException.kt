package com.safehill.kclient.network.exceptions

import com.github.kittinunf.fuel.core.HttpException
import com.safehill.kclient.network.remote.SafehillHttpStatusCode

data class SafehillHttpException(
    val statusCode: SafehillHttpStatusCode?,
    override val message: String,
    val httpException: HttpException
) : Exception(message, httpException) {
    constructor(
        statusCode: Int,
        message: String,
        httpException: HttpException = HttpException(statusCode, message)
    ) : this(
        SafehillHttpStatusCode.fromInt(statusCode),
        "$statusCode: $message",
        httpException
    )
}