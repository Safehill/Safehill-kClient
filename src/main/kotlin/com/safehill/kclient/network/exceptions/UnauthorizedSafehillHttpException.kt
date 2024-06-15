package com.safehill.kclient.network.exceptions

val UnauthorizedSafehillHttpException = SafehillHttpException(
    statusCode = 401,
    message = "unauthorized",
)

val ConflictSafehillHttpException = SafehillHttpException(
    statusCode = 409,
    message = "conflict",
)
