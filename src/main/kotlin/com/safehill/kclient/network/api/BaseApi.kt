package com.safehill.kclient.network.api

import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.deserializers.StringDeserializer
import com.github.kittinunf.fuel.core.extensions.AuthenticatedRequest
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.response
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import com.github.kittinunf.result.Result
import com.safehill.kclient.models.GenericFailureResponse
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.exceptions.SafehillError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

typealias UserFlow = StateFlow<LocalUser?>

interface BaseApi {
    val userFlow: UserFlow
}

suspend inline fun <reified Request : Any> BaseApi.postRequestForStringResponse(
    endPoint: String,
    request: Request? = null,
    authenticationRequired: Boolean = true
): String {
    return fireRequestForStringResponse(
        request = request,
        requestMethod = RequestMethod.Post,
        endPoint = endPoint,
        authenticationRequired = authenticationRequired
    )
}


suspend inline fun <reified Request : Any> BaseApi.fireRequestForStringResponse(
    requestMethod: RequestMethod,
    endPoint: String,
    request: Request? = null,
    authenticationRequired: Boolean = true
): String {
    return fireRequest(
        requestMethod = requestMethod,
        endPoint = endPoint,
        request = request,
        authenticationRequired = authenticationRequired,
        // Cannot use kotlinx.serialization's String.serializer() because it expects quoted strings.
        // String.serializer() is not able to decode empty body to empty strings.
        serializer = StringDeserializer()
    )
}

suspend inline fun <reified Request : Any, reified Response : Any> BaseApi.postRequestForObjectResponse(
    endPoint: String,
    request: Request? = null,
    authenticationRequired: Boolean = true
): Response {
    return fireRequestForObjectResponse<Request, Response>(
        requestMethod = RequestMethod.Post,
        endPoint = endPoint,
        request = request,
        authenticationRequired = authenticationRequired
    )
}

suspend inline fun <reified Request : Any, reified Response : Any> BaseApi.fireRequestForObjectResponse(
    requestMethod: RequestMethod,
    endPoint: String,
    request: Request? = null,
    authenticationRequired: Boolean = true
): Response {
    @OptIn(ExperimentalSerializationApi::class)
    val ignorantJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    return fireRequest(
        requestMethod = requestMethod,
        endPoint = endPoint,
        request = request,
        authenticationRequired = authenticationRequired,
        serializer = kotlinxDeserializerOf(serializer<Response>(), json = ignorantJson)
    )
}

suspend inline fun <reified Request : Any, reified Response : Any> BaseApi.fireRequest(
    requestMethod: RequestMethod,
    endPoint: String,
    request: Request? = null,
    serializer: Deserializable<Response>,
    authenticationRequired: Boolean = true
): Response {
    return withContext(Dispatchers.IO) {
        createRequest<Request>(
            requestMethod = requestMethod,
            endPoint = endPoint,
            request = request,
            authenticationRequired = authenticationRequired
        ).response(serializer)
            .getOrThrow()
    }
}

sealed class RequestMethod {
    data object Post : RequestMethod()
    data object Delete : RequestMethod()
    data class Get(val query: List<Pair<String, Any>>) : RequestMethod()
}

inline fun <reified Req> BaseApi.createRequest(
    endPoint: String,
    request: Req? = null,
    requestMethod: RequestMethod,
    authenticationRequired: Boolean = true
): Request {
    return endPoint
        .run {
            when (requestMethod) {
                RequestMethod.Post -> httpPost()
                RequestMethod.Delete -> httpDelete()
                is RequestMethod.Get -> httpGet(requestMethod.query)
            }
        }
        .apply {
            if (authenticationRequired) {
                authentication()
                    .bearer(token = userFlow.value?.authToken)
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
        404 -> SafehillError.ClientError.NotFound()
        405 -> SafehillError.ClientError.MethodNotAllowed
        409 -> SafehillError.ClientError.Conflict
        501 -> SafehillError.ServerError.NotImplemented
        503 -> SafehillError.ServerError.BadGateway
        else -> {
            val responseMessage = this.response.data.toString(Charsets.UTF_8)
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