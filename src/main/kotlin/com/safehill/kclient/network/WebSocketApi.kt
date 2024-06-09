package com.safehill.kclient.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.Url

/**
 * Obtain [WebSocketApi]'s instance from configured [com.safehill.SafehillClient]
 */
class WebSocketApi internal constructor(
    private val url: Url
) {
    private val httpClient = HttpClient {
        install(Logging)
        install(WebSockets)
    }

}