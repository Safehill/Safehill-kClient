package com.safehill.safehillclient.sdk

import com.safehill.kclient.network.api.BaseOpenApi
import com.safehill.kclient.network.api.auth.AuthApiImpl
import com.safehill.kclient.network.remote.RemoteServerEnvironment
import com.safehill.kclient.utils.setupBouncyCastle
import com.safehill.safehillclient.sdk.backgroundsync.ClientOptions
import com.safehill.safehillclient.sdk.factory.serverproxy.HttpClientFactory
import com.safehill.safehillclient.sdk.factory.serverproxy.NetworkModuleFactory
import com.safehill.safehillclient.sdk.module.sdk.ClientModule
import com.safehill.safehillclient.sdk.platform.PlatformModule
import com.safehill.safehillclient.sdk.platform.UserModule
import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol

class SafehillSDKBuilder(
    private val remoteServerEnvironment: RemoteServerEnvironment,
    private val platformModule: PlatformModule,
    private val userModule: UserModule,
    private val clientOptions: ClientOptions = ClientOptions(),
    private val configureHttpClient: HttpClient.() -> Unit = { }
) {

    fun build(): SafehillClient {
        setupBouncyCastle()
        val httpClient = HttpClientFactory(
            safehillLogger = platformModule.safehillLogger,
            configureHttpClient = configureHttpClient
        ).create(remoteServerEnvironment)
        val baseOpenApi = object : BaseOpenApi {
            override val client: HttpClient = httpClient
        }
        val socketUrl = buildWsURL(remoteServerEnvironment)
        val networkModuleFactory = NetworkModuleFactory(
            remoteServerEnvironment = remoteServerEnvironment,
            client = httpClient,
            socketUrl = socketUrl,
            platformModule = platformModule,
            userModule = userModule
        )
        val clientModule = ClientModule(
            platformModule = platformModule,
            clientOptions = clientOptions,
            userModule = userModule,
            networkModuleFactory = networkModuleFactory
        )
        return SafehillClient(
            clientModule = clientModule,
            authApi = AuthApiImpl(baseOpenApi)
        )
    }


    companion object {
        fun buildWsURL(remoteServerEnvironment: RemoteServerEnvironment) =
            URLBuilder().apply {
                this.host = remoteServerEnvironment.hostName
                this.protocol = when (remoteServerEnvironment) {
                    is RemoteServerEnvironment.Development -> URLProtocol.WS
                    is RemoteServerEnvironment.Staging -> URLProtocol.WSS
                    RemoteServerEnvironment.Production -> URLProtocol.WSS
                }
                this.port = remoteServerEnvironment.port
            }.build()
    }
}
