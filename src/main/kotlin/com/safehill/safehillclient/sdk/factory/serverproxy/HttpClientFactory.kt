package com.safehill.safehillclient.sdk.factory.serverproxy

import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.network.remote.RemoteServerEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class HttpClientFactory(
    private val safehillLogger: SafehillLogger,
    private val configureHttpClient: HttpClient.() -> Unit = {}
) {

    fun create(remoteServerEnvironment: RemoteServerEnvironment): HttpClient {
        return HttpClient(CIO) {
            defaultRequest {
                url(buildRestApiUrl(remoteServerEnvironment).buildString())
                contentType(ContentType.Application.Json)
            }
            install(HttpTimeout) {
                this.connectTimeoutMillis = 10000
                this.requestTimeoutMillis = 30000
            }
            install(ContentNegotiation) {
                val ignorantJson = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
                json(ignorantJson)
            }
            install(Logging) {
                level = LogLevel.ALL
                this.logger = object : Logger {
                    override fun log(message: String) {
                        safehillLogger.info(message)
                    }
                }
            }
        }.apply { configureHttpClient() }
    }


    private fun buildRestApiUrl(remoteServerEnvironment: RemoteServerEnvironment) =
        URLBuilder().apply {
            this.host = remoteServerEnvironment.hostName
            this.protocol = when (remoteServerEnvironment) {
                is RemoteServerEnvironment.Development -> URLProtocol.HTTP
                is RemoteServerEnvironment.Staging -> URLProtocol.HTTPS
                RemoteServerEnvironment.Production -> URLProtocol.HTTPS
            }
            this.port = remoteServerEnvironment.port
        }
}