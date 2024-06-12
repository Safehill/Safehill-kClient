package com.safehill.kclient.network

import com.safehill.kclient.models.dtos.websockets.WebSocketMessage
import com.safehill.kclient.models.serde.WebSocketMessageDeserializer
import com.safehill.kclient.models.users.LocalUser
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

/**
 * Obtain [WebSocketApi]'s instance from configured [com.safehill.SafehillClient]
 */
class WebSocketApi internal constructor(
    private val socketUrl: Url
) {
    private val httpClient = HttpClient(CIO) {
        install(Logging) {
            this.logger = object : Logger {
                override fun log(message: String) {
                    //todo discuss way or properly logging from library
                    println("Socket message $message")
                }
            }
        }
        install(WebSockets)
    }

    suspend fun connectToSocket(
        currentUser: LocalUser,
        deviceId: String,
        onSocketData: (WebSocketMessage) -> Unit
    ) {
        monitorSocketConnection {
            httpClient.webSocket(
                method = HttpMethod.Get,
                host = socketUrl.host,
                path = "ws/messages",
                port = socketUrl.port,
                request = {
                    this.headers["Authorization"] = "Bearer ${currentUser.authToken}"
                    parameter("deviceId", deviceId)
                }
            ) {
                this.incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val socketData = Json.decodeFromString(
                            deserializer = WebSocketMessageDeserializer,
                            string = frame.readText()
                        )
                        onSocketData(socketData)
                    }
                }
            }
        }
    }

    /**
     * The [block] should not complete till there is an active socket connection.
     * Connect to socket inside the [block] and suspend forever.
     * The connection will be closed once the coroutine is cancelled.
     * If any error occurs, the [block] is executed again with certain delay to re establish socket connection.
     */
    private suspend fun monitorSocketConnection(block: suspend () -> Unit) {
        var retryDelay = 1
        while (coroutineContext.isActive) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                delay(1.seconds * retryDelay)
                retryDelay = minOf(MAX_RETRY_DELAY, retryDelay * 2)
            }
        }
    }

    companion object {
        private const val MAX_RETRY_DELAY: Int = 8
    }
}