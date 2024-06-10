package com.safehill.kclient.network

import com.safehill.kclient.models.serde.WebSocketMessageDeserializer
import com.safehill.kclient.models.users.LocalUser
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json

/**
 * Obtain [WebSocketApi]'s instance from configured [com.safehill.SafehillClient]
 */
class WebSocketApi internal constructor(
    private val url: Url
) {
    private val httpClient = HttpClient {
        install(Logging) {
            this.logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
        }
        install(WebSockets)
    }

    suspend fun connectToSocket(
        currentUser: LocalUser,
        deviceId: String
    ) {
        httpClient.webSocket(
            method = HttpMethod.Get,
            host = url.host,
            path = "ws/messages",
            port = url.port,
            request = {
                this.headers["Authorization"] = "Bearer ${currentUser.authToken}"
                parameter("deviceId", deviceId)
            }
        ) {
            this.incoming.consumeEach {
                val data = it.data.decodeToString()
               val socketData=  Json.decodeFromString(WebSocketMessageDeserializer, data)
                println("The data is $socketData")
            }
        }
    }

}