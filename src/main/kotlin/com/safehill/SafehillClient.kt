package com.safehill

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.interceptors.LogRequestInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogResponseInterceptor
import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.controllers.UserInteractionController
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.ServerProxyImpl
import com.safehill.kclient.network.WebSocketApi
import com.safehill.kclient.network.local.LocalServerInterface
import com.safehill.kclient.network.remote.RemoteServer
import com.safehill.kclient.network.remote.RemoteServerEnvironment
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class SafehillClient private constructor(
    val serverProxy: ServerProxy,
    val webSocketApi: WebSocketApi,
    val currentUser: LocalUser
) {

    val interactionController by lazy {
        UserInteractionController(
            serverProxy = serverProxy,
            currentUser = currentUser
        )
    }

    val userController by lazy {
        UserController(
            serverProxy = serverProxy
        )
    }

    suspend fun connectToSocket(deviceId: String) {
        webSocketApi.connectToSocket(deviceId = deviceId, currentUser = currentUser)
    }

    class Builder(
        private val localServer: LocalServerInterface,
        private val currentUser: LocalUser,
        private val remoteServerEnvironment: RemoteServerEnvironment
    ) {
        private fun buildWsURL() = URLBuilder().apply {
            this.host = remoteServerEnvironment.hostName
            this.protocol = when (remoteServerEnvironment) {
                is RemoteServerEnvironment.Development -> URLProtocol.WS
                RemoteServerEnvironment.Production -> URLProtocol.WSS
            }
            this.port = remoteServerEnvironment.port
        }.build()

        private fun buildRestApiUrl() = URLBuilder().apply {
            this.host = remoteServerEnvironment.hostName
            this.protocol = when (remoteServerEnvironment) {
                is RemoteServerEnvironment.Development -> URLProtocol.HTTP
                RemoteServerEnvironment.Production -> URLProtocol.HTTPS
            }
            this.port = remoteServerEnvironment.port
        }

        private fun setUpFuelConfiguration() {
            FuelManager.instance.basePath = buildRestApiUrl().toString()
            FuelManager.instance.baseHeaders = mapOf("Content-type" to "application/json")
            FuelManager.instance.timeoutInMillisecond = 10000
            FuelManager.instance.timeoutReadInMillisecond = 30000

            // The client should control whether they want logging or not
            // Printing for now
            FuelManager.instance.addRequestInterceptor(LogRequestInterceptor)
            FuelManager.instance.addResponseInterceptor(LogResponseInterceptor)
        }

        private fun setupBouncyCastle() {
            // Android registers its own BC provider. As it might be outdated and might not include
            // all needed ciphers, we substitute it with a known BC bundled in the app.
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null) {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            }
            Security.addProvider(BouncyCastleProvider())
        }

        fun build(): SafehillClient {
            setupBouncyCastle()
            setUpFuelConfiguration()
            return SafehillClient(
                serverProxy = ServerProxyImpl(
                    localServer = localServer,
                    remoteServer = RemoteServer(
                        requestor = currentUser
                    ),
                    requestor = currentUser
                ),
                webSocketApi = WebSocketApi(
                    socketUrl = buildWsURL()
                ),
                currentUser = currentUser
            )
        }
    }
}