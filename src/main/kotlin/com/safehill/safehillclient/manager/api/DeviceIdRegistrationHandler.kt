package com.safehill.safehillclient.manager.api

import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.util.safeApiCall
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.module.client.ClientModule
import com.safehill.safehillclient.module.client.serverProxy
import com.safehill.safehillclient.utils.api.deviceid.DeviceIdProvider

class DeviceIdRegistrationHandler(
    private val serverProxy: ServerProxy,
    private val deviceIdProvider: DeviceIdProvider,
    private val safehillLogger: SafehillLogger
) : UserObserver {

    suspend fun registerDeviceId() {
        val remoteResult = safeApiCall {
            serverProxy.registerDevice(
                deviceId = deviceIdProvider.getDeviceID(),
                // Push notifications not supported for now.
                token = ""
            )
        }
        remoteResult
            .onSuccess {
                safehillLogger.info("Successfully registered device id.")
            }.onFailure {
                safehillLogger.info("Failed to register device id.")
            }
    }

    override suspend fun userSet(user: LocalUser) {
        registerDeviceId()
    }

    override fun clearUser(clearPersistence: Boolean) {}

    class Factory(private val clientModule: ClientModule) {
        fun create(): DeviceIdRegistrationHandler {
            return DeviceIdRegistrationHandler(
                serverProxy = clientModule.serverProxy,
                deviceIdProvider = clientModule.platformModule.deviceIdProvider,
                safehillLogger = clientModule.platformModule.safehillLogger
            )
        }
    }

}