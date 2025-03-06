package com.safehill.safehillclient.factory

import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.network.ServerProxyImpl
import com.safehill.kclient.network.WebSocketApi
import com.safehill.kclient.network.local.LocalServerInterface
import com.safehill.kclient.network.remote.RemoteServer
import com.safehill.kclient.network.remote.RemoteServerEnvironment
import com.safehill.kclient.util.Provider
import com.safehill.safehillclient.backgroundsync.NetworkModule
import com.safehill.safehillclient.module.platform.PlatformModule
import com.safehill.safehillclient.module.platform.UserModule
import io.ktor.client.HttpClient
import io.ktor.http.Url
import java.util.concurrent.ConcurrentHashMap

class NetworkModuleFactory(
    private val remoteServerEnvironment: RemoteServerEnvironment,
    private val client: HttpClient,
    private val socketUrl: Url,
    private val userModule: UserModule,
    private val platformModule: PlatformModule
) {

    private val localServerCache = ConcurrentHashMap<String, LocalServerInterface>()

    fun create(
        userProvider: UserProvider
    ): NetworkModule {
        val serverProxy = ServerProxyImpl(
            localServerProvider = Provider {
                val currentUser = userProvider.get()
                localServerCache.getOrPut(currentUser.identifier) {
                    userModule.getLocalServer(currentUser)
                }
            },
            remoteServer = RemoteServer(
                userProvider = userProvider,
                client = client,
                safehillLogger = platformModule.safehillLogger
            )
        )
        val webSocketApi = WebSocketApi(
            socketUrl = socketUrl,
            logger = platformModule.safehillLogger
        )
        return NetworkModule(
            serverProxy = serverProxy,
            webSocketApi = webSocketApi,
            remoteServerEnvironment = remoteServerEnvironment
        )
    }
}