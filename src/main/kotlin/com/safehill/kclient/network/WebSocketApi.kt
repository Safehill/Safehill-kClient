package com.safehill.kclient.network

import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.dtos.websockets.WebSocketMessage
import com.safehill.kclient.models.serde.WebSocketMessageDeserializer
import com.safehill.kclient.models.users.LocalUser
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

/**
 * Obtain [WebSocketApi]'s instance from configured [com.safehill.SafehillClient]
 */
class WebSocketApi(
    private val socketUrl: Url,
    private val logger: SafehillLogger
) {

    private val connectionMutex = Mutex()

    private val _socketMessage = MutableSharedFlow<WebSocketMessage>(
        extraBufferCapacity = 32
    )
    val socketMessages = _socketMessage.asSharedFlow()

    private val httpClient = HttpClient(CIO) {
        install(Logging) {
            this.logger = safehillClientLogger
            this.level = LogLevel.ALL
        }
        install(WebSockets)
    }

    private suspend fun connectToSocketInternal(
        currentUser: LocalUser,
        deviceId: String
    ) {
        httpClient.webSocket(
            method = HttpMethod.Get,
            host = socketUrl.host,
            path = "ws/messages",
            port = socketUrl.port,
            request = {
                this.bearerAuth(
                    currentUser.authToken ?: error("Trying to connect to socket without authToken")
                )
                this.url {
                    this.protocol = socketUrl.protocol
                }
                parameter("deviceId", deviceId)
            }
        ) {
            val socketSession = this
            coroutineScope {
                launch {
                    socketSession.incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            val socketData = Json.decodeFromString(
                                deserializer = WebSocketMessageDeserializer,
                                string = frame.readText()
                            )
                            logger.info("Socket message ${frame.readText()} parsed to $socketData")
                            _socketMessage.emit(socketData)
                        }
                    }
                }
                launch {
                    while (isActive) {
                        socketSession.outgoing.send(Frame.Ping("ping".toByteArray()))
                        delay(5.seconds)
                    }
                }
            }
        }
    }

    suspend fun connectToSocket(
        currentUser: LocalUser,
        deviceId: String
    ) {
        try {
            if (connectionMutex.tryLock()) {
                monitorSocketConnection {
                    connectToSocketInternal(
                        currentUser = currentUser,
                        deviceId = deviceId
                    )
                }
            }
        } finally {
            connectionMutex.unlock()
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
                logger.error("Socket Connection error = $e")
            }
        }
    }

    private val safehillClientLogger
        get() = object : Logger {
            override fun log(message: String) {
                logger.info(message)
            }
        }

    companion object {
        private const val MAX_RETRY_DELAY: Int = 8
    }
}