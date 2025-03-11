package com.safehill.kclient.network.api

import com.safehill.kclient.models.GenericFailureResponse
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.exceptions.SafehillError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HeadersBuilder
import io.ktor.http.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface BaseOpenApi {
    val client: HttpClient
}

interface BaseApi : BaseOpenApi {
    val requestor: LocalUser
}

suspend inline fun <reified Request : Any, reified Response : Any> BaseOpenApi.postRequestForResponse(
    endPoint: String,
    request: Request? = null
): Response {
    return fireRequest<Request, Response>(
        requestMethod = RequestMethod.Post,
        endPoint = endPoint,
        request = request
    )
}

suspend inline fun <reified Request : Any> BaseOpenApi.postRequest(
    endPoint: String,
    request: Request? = null
) {
    return postRequestForResponse<Request, Unit>(
        endPoint = endPoint,
        request = request
    )
}

suspend inline fun <reified Request : Any, reified Response : Any> BaseOpenApi.fireRequest(
    requestMethod: RequestMethod,
    endPoint: String,
    request: Request? = null
): Response {
    return withContext(Dispatchers.IO) {
        val requestBuilder = getRequestBuilder(
            endPoint = endPoint,
            request = request,
            requestMethod = requestMethod
        )
        when (requestMethod) {
            RequestMethod.Post -> {
                client.post(requestBuilder)
            }

            RequestMethod.Delete -> {
                client.delete(requestBuilder)
            }

            is RequestMethod.Get -> {
                client.get(requestBuilder)
            }
        }.getOrThrow<Response>()
    }
}

sealed class RequestMethod {
    data object Post : RequestMethod()
    data object Delete : RequestMethod()
    data class Get(val query: List<Pair<String, String>>) : RequestMethod()
}

inline fun <reified Req> BaseOpenApi.getRequestBuilder(
    endPoint: String,
    request: Req? = null,
    requestMethod: RequestMethod
): HttpRequestBuilder {
    val api = this

    return HttpRequestBuilder()
        .apply {
            url {
                path(endPoint)
                if (requestMethod is RequestMethod.Get) {
                    requestMethod.query.forEach {
                        parameters.append(it.first, it.second)
                    }
                }
            }
            setBody(request)
            headers {
                if (api is BaseApi) {
                    bearer(token = api.requestor.authToken)
                }
            }
        }
}

fun HeadersBuilder.bearer(token: String?) {
    if (token == null) throw SafehillError.ClientError.Unauthorized
    this["Authorization"] = "Bearer $token"
}

suspend fun HttpResponse.getSafehillError(): SafehillError {
    return when (this.status.value) {
        401 -> SafehillError.ClientError.Unauthorized
        402 -> SafehillError.ClientError.PaymentRequired
        404 -> SafehillError.ClientError.NotFound
        405 -> SafehillError.ClientError.MethodNotAllowed
        409 -> SafehillError.ClientError.Conflict
        501 -> SafehillError.ServerError.NotImplemented
        503 -> SafehillError.ServerError.BadGateway
        else -> {
            val responseMessage = this.bodyAsBytes().toString(Charsets.UTF_8)
            val message = try {
                val failure = Json.decodeFromString<GenericFailureResponse>(responseMessage)
                failure.reason
            } catch (e: SerializationException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
            if (this.status.value in 400..500) {
                SafehillError.ClientError.BadRequest(message ?: "Bad or malformed request")
            } else {
                SafehillError.ServerError.Generic(message ?: "A server error occurred")
            }
        }
    }
}

suspend inline fun <reified T> HttpResponse.getOrThrow(): T {
    val code = this.status.value
    if (code in 200..299) return this.body<T>() else throw this.getSafehillError()
}