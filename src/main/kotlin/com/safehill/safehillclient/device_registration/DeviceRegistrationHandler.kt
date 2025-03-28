package com.safehill.safehillclient.device_registration

import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.util.safeApiCall
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.module.client.ClientModule
import com.safehill.safehillclient.utils.api.deviceid.DeviceIdProvider

class DeviceRegistrationHandler(
    private val serverProxy: ServerProxy,
    private val deviceIdProvider: DeviceIdProvider,
    private val safehillLogger: SafehillLogger,
    private val deviceRegistrationStrategy: DeviceRegistrationStrategy
) : UserObserver {

    private val deviceRegistrationCache = run {
        val onChangeStrategy = deviceRegistrationStrategy as? DeviceRegistrationStrategy.OnChange
        onChangeStrategy?.deviceRegistrationCache
    }

    private suspend fun registerDeviceId() {
        val token = deviceRegistrationStrategy
            .pushTokenConfig
            .getToken()

        val registrationInfo = DeviceRegistrationInfo(
            token = token,
            userId = serverProxy.requestor.identifier,
            deviceId = deviceIdProvider.getDeviceID()
        )

        val cachedRegistrationInfo = deviceRegistrationCache?.getRegistrationInfo()
        if (cachedRegistrationInfo != registrationInfo) {
            deviceRegistrationCache?.cacheRegistrationInfo(DeviceRegistrationInfo())
            val remoteResult = safeApiCall {
                serverProxy.registerDevice(
                    deviceId = registrationInfo.deviceId,
                    token = token
                )
            }
            remoteResult
                .onSuccess {
                    deviceRegistrationCache?.cacheRegistrationInfo(registrationInfo)
                    safehillLogger.info("Successfully registered device id with info $registrationInfo")
                }.onFailure {
                    safehillLogger.info("Failed to register device id.")
                }
        }
    }

    override suspend fun userLoggedIn(user: LocalUser) {
        registerDeviceId()
    }

    override fun userLoggedOut() {}

    class Factory(
        private val clientModule: ClientModule
    ) {
        fun create(): DeviceRegistrationHandler {
            return DeviceRegistrationHandler(
                serverProxy = clientModule.networkModule.serverProxy,
                deviceIdProvider = clientModule.platformModule.deviceIdProvider,
                safehillLogger = clientModule.clientOptions.safehillLogger,
                deviceRegistrationStrategy = clientModule.configs.deviceRegistrationStrategy
            )
        }
    }
}