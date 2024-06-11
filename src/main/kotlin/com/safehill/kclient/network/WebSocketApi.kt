package com.safehill.kclient.network

import com.safehill.kclient.models.dtos.websockets.WebSocketMessage
import com.safehill.kclient.models.serde.WebSocketMessageDeserializer
import com.safehill.kclient.models.users.LocalUser
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json

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
                    println("Socket message $message")
                }
            }
        }
        install(WebSockets)
    }

    private suspend fun getSocketSession(
        currentUser: LocalUser,
        deviceId: String
    ): DefaultClientWebSocketSession {
        return httpClient.webSocketSession(
            method = HttpMethod.Get,
            host = socketUrl.host,
            path = "ws/messages",
            port = socketUrl.port,
            block = {
                this.headers["Authorization"] = "Bearer ${currentUser.authToken}"
                parameter("deviceId", deviceId)
            }
        )
    }

    suspend fun connectToSocket(
        currentUser: LocalUser,
        deviceId: String,
        onSocketData: (WebSocketMessage) -> Unit
    ) {
        val session = getSocketSession(
            currentUser = currentUser,
            deviceId = deviceId
        )
        session.incoming.consumeEach { frame ->
            if (frame is Frame.Text) {
                val socketData = Json.decodeFromString(
                    deserializer = WebSocketMessageDeserializer,
                    string = frame.readText()
                )
                onSocketData(socketData)
            }
        }
    }

    companion object {
        private const val MAX_RETRY_DELAY: Int = 8
    }
}