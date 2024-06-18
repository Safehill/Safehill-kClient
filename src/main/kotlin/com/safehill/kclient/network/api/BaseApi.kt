package com.safehill.kclient.network.api

import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.extensions.AuthenticatedRequest
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.serialization.responseObject
import com.github.kittinunf.result.Result
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.exceptions.SafehillHttpException
import com.safehill.kclient.network.exceptions.UnauthorizedSafehillHttpException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface BaseApi {
    val requestor: LocalUser
    fun <T> ResponseResultOf<T>.getOrElseOnSafehillException(transform: (SafehillHttpException) -> T): T {
        return try {
            this.getOrThrow()
        } catch (e: Exception) {
            if (e is SafehillHttpException) {
                transform(e)
            } else {
                throw e
            }
        }
    }

    fun <T, R> ResponseResultOf<T>.getMappingOrThrow(transform: (T) -> R): R {
        val value = getOrThrow()
        return transform(value)
    }

    fun <T> ResponseResultOf<T>.getOrThrow(): T {
        return when (val result = this.third) {
            is Result.Success -> result.value
            is Result.Failure -> {
                val fuelError = result.error
                val exception = fuelError.exception
                throw if (exception is HttpException) {
                    SafehillHttpException(
                        fuelError.response.statusCode,
                        fuelError.response.responseMessage,
                        exception
                    )
                } else {
                    exception
                }
            }
        }
    }

}


inline fun <reified Request : Any, reified Response : Any> BaseApi.postRequest(
    endPoint: String,
    request: Request?,
    authenticationRequired: Boolean = true
): Response {
    @OptIn(ExperimentalSerializationApi::class)
    val ignorantJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    return endPoint
        .httpPost()
        .apply {
            if (authenticationRequired) {
                authentication()
                    .bearer(token = requestor.authToken)
            }
            if (request != null) {
                body(Json.encodeToString(request))
            }
        }
        .responseObject<Response>(json = ignorantJson)
        .getOrThrow()

}

fun AuthenticatedRequest.bearer(token: String?): Request {
    if (token == null) throw UnauthorizedSafehillHttpException
    return this.bearer(token)
}
