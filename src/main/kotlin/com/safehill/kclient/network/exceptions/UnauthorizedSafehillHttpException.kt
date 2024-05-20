package com.safehill.kclient.network.exceptions

import com.safehill.kclient.network.exceptions.SafehillHttpException

val UnauthorizedSafehillHttpException = SafehillHttpException(
    statusCode = 401,
    message = "unauthorized",
)
