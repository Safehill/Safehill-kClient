package com.safehill.safehillclient.manager.api

import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.WebSocketApi
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.module.client.ClientModule
import com.safehill.safehillclient.module.client.UserScope
import com.safehill.safehillclient.utils.api.deviceid.DeviceIdProvider
import kotlinx.coroutines.launch

class SocketManager(
    private val webSocketApi: WebSocketApi,
    private val userScope: UserScope,
    private val deviceIdProvider: DeviceIdProvider
) : UserObserver {

    override suspend fun userSet(user: LocalUser) {
        userScope.launch {
            webSocketApi.connectToSocket(
                currentUser = user,
                deviceId = deviceIdProvider.getDeviceID()
            )
        }
    }

    override fun clearUser(clearPersistence: Boolean) {

    }

    class Factory(
        private val clientModule: ClientModule
    ) {
        fun create(): SocketManager {
            return SocketManager(
                webSocketApi = clientModule.networkModule.webSocketApi,
                userScope = clientModule.clientOptions.userScope,
                deviceIdProvider = clientModule.platformModule.deviceIdProvider
            )
        }

    }
}