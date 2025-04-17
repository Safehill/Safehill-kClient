package com.safehill.safehillclient.device_registration

sealed class DeviceRegistrationStrategy(
    open val pushTokenConfig: PushTokenConfig
) {
    data class OnEveryLogin(
        override val pushTokenConfig: PushTokenConfig
    ) : DeviceRegistrationStrategy(pushTokenConfig)

    class OnChange(
        val deviceRegistrationCache: DeviceRegistrationCache,
        override val pushTokenConfig: PushTokenConfig
    ) : DeviceRegistrationStrategy(pushTokenConfig)
}

sealed class PushTokenConfig {

    data class Active(
        val pushTokenProvider: PushTokenProvider,
    ) : PushTokenConfig()

    object Inactive : PushTokenConfig()

    suspend fun getToken() = when (this) {
        is Active -> pushTokenProvider.get()
        Inactive -> null
    }
}

interface PushTokenProvider {
    suspend fun get(): String
}

interface DeviceRegistrationCache {
    suspend fun cacheRegistrationInfo(deviceRegistrationInfo: DeviceRegistrationInfo)
    suspend fun getRegistrationInfo(): DeviceRegistrationInfo
}

data class DeviceRegistrationInfo(
    val token: String? = null,
    val userId: String = "",
    val deviceId: String = ""
)
