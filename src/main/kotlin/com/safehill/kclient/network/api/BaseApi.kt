package com.safehill.kclient.network.api

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.extensions.AuthenticatedRequest
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.serialization.responseObject
import com.github.kittinunf.result.Result
import com.safehill.kclient.models.GenericFailureResponse
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.exceptions.SafehillError
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface BaseApi {
    val requestor: LocalUser
}

inline fun <reified Request : Any> BaseApi.postForResponseString(
    endPoint: String,
    request: Request? = null,
    authenticationRequired: Boolean = true
): String {
    return createPostRequest(
        endPoint = endPoint,
        request = request,
        authenticationRequired = authenticationRequired
    ).responseString()
        .getOrThrow()
}

inline fun <reified Request : Any, reified Response : Any> BaseApi.postForResponseObject(
    endPoint: String,
    request: Request? = null,
    authenticationRequired: Boolean = true
): Response {
    @OptIn(ExperimentalSerializationApi::class)
    val ignorantJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    return createPostRequest<Request>(
        endPoint = endPoint,
        request = request,
        authenticationRequired = authenticationRequired
    ).responseObject<Response>(json = ignorantJson)
        .getOrThrow()
}

inline fun <reified Req> BaseApi.createPostRequest(
    endPoint: String,
    request: Req? = null,
    authenticationRequired: Boolean = true
): Request {
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
}

fun AuthenticatedRequest.bearer(token: String?): Request {
    if (token == null) throw SafehillError.ClientError.Unauthorized
    return this.bearer(token)
}

private fun FuelError.getSafehillError(): SafehillError {
    return when (this.response.statusCode) {
        401 -> SafehillError.ClientError.Unauthorized
        402 -> SafehillError.ClientError.PaymentRequired
        404 -> SafehillError.ClientError.NotFound
        405 -> SafehillError.ClientError.MethodNotAllowed
        409 -> SafehillError.ClientError.Conflict
        501 -> SafehillError.ServerError.NotImplemented
        503 -> SafehillError.ServerError.BadGateway
        else -> {
            val responseMessage = this.response.responseMessage
            val message = try {
                val failure = Json.decodeFromString<GenericFailureResponse>(responseMessage)
                failure.reason
            } catch (e: SerializationException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
            if (this.response.statusCode in 400..500) {
                SafehillError.ClientError.BadRequest(message ?: "Bad or malformed request")
            } else {
                SafehillError.ServerError.Generic(message ?: "A server error occurred")
            }
        }
    }
}


fun <T, R> ResponseResultOf<T>.getMappingOrThrow(transform: (T) -> R): R {
    val value = getOrThrow()
    return transform(value)
}

fun <T> ResponseResultOf<T>.getOrElseOnSafehillError(transform: (SafehillError) -> T): T {
    return try {
        this.getOrThrow()
    } catch (e: Exception) {
        if (e is SafehillError) {
            transform(e)
        } else {
            throw e
        }
    }
}

fun <T> ResponseResultOf<T>.getOrThrow(): T {
    return when (val result = this.third) {
        is Result.Success -> result.value
        is Result.Failure -> {
            val fuelError = result.error
            val exception = fuelError.exception
            throw if (exception is HttpException) {
                fuelError.getSafehillError()
            } else {
                exception
            }
        }
    }
}