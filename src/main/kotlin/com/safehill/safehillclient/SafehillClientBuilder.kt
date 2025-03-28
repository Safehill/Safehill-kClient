package com.safehill.safehillclient

import com.safehill.kclient.network.api.BaseOpenApi
import com.safehill.kclient.network.api.auth.AuthApiImpl
import com.safehill.kclient.network.remote.RemoteServerEnvironment
import com.safehill.kclient.utils.setupBouncyCastle
import com.safehill.safehillclient.data.user.api.UserStorage
import com.safehill.safehillclient.device_registration.DeviceRegistrationStrategy
import com.safehill.safehillclient.factory.HttpClientFactory
import com.safehill.safehillclient.factory.NetworkModuleFactory
import com.safehill.safehillclient.manager.ClientManager
import com.safehill.safehillclient.model.auth.state.AuthStateHolder
import com.safehill.safehillclient.module.client.ClientModule
import com.safehill.safehillclient.module.config.ClientOptions
import com.safehill.safehillclient.module.config.Configs
import com.safehill.safehillclient.module.platform.PlatformModule
import com.safehill.safehillclient.module.platform.UserModule
import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol

class SafehillClientBuilder(
    private val remoteServerEnvironment: RemoteServerEnvironment,
    private val platformModule: PlatformModule,
    private val userModule: UserModule,
    private val userStorage: UserStorage,
    private val deviceRegistrationStrategy: DeviceRegistrationStrategy,
    private val clientOptions: ClientOptions = ClientOptions(),
    private val configureHttpClient: HttpClient.() -> Unit = { }
) {

    fun build(): SafehillClient {
        setupBouncyCastle()
        val httpClient = HttpClientFactory(
            safehillLogger = clientOptions.safehillLogger,
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
            networkModuleFactory = networkModuleFactory,
            configs = Configs(
                deviceRegistrationStrategy = deviceRegistrationStrategy
            )
        )
        val clientManager = ClientManager.Factory(clientModule).create()
        return SafehillClient(
            clientModule = clientModule,
            authApi = AuthApiImpl(baseOpenApi),
            clientManager = clientManager,
            repositories = clientManager.repositories,
            authStateHolder = AuthStateHolder(),
            userStorage = userStorage
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
