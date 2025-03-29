package com.safehill.safehillclient.manager

import com.safehill.kclient.models.users.LocalUser
import com.safehill.safehillclient.backgroundsync.SafehillSyncManager
import com.safehill.safehillclient.data.user.api.DefaultUserObserverRegistry
import com.safehill.safehillclient.data.user.api.UserDataManager
import com.safehill.safehillclient.data.user.api.UserObserverRegistry
import com.safehill.safehillclient.device_registration.DefaultDeviceRegistrationHandler
import com.safehill.safehillclient.device_registration.DeviceRegistrationHandler
import com.safehill.safehillclient.manager.api.SocketManager
import com.safehill.safehillclient.manager.dependencies.Repositories
import com.safehill.safehillclient.manager.dependencies.SdkRepositories
import com.safehill.safehillclient.module.client.ClientModule

class ClientManager(
    val repositories: Repositories,
    private val defaultDeviceRegistrationHandler: DefaultDeviceRegistrationHandler,
    private val socketManager: SocketManager,
    private val safehillSyncManager: SafehillSyncManager,
    private val userDataManager: UserDataManager
) : UserObserverRegistry by DefaultUserObserverRegistry(
    repositories,
    defaultDeviceRegistrationHandler,
    safehillSyncManager,
    socketManager
) {

    val deviceRegistrationHandler: DeviceRegistrationHandler = defaultDeviceRegistrationHandler

    suspend fun clearUserData(user: LocalUser) {
        userDataManager.clear(user)
    }

    class Factory(
        private val clientModule: ClientModule
    ) {
        fun create(): ClientManager {
            return ClientManager(
                repositories = SdkRepositories
                    .Factory(clientModule)
                    .create(),
                defaultDeviceRegistrationHandler = DefaultDeviceRegistrationHandler
                    .Factory(clientModule)
                    .create(),
                socketManager = SocketManager
                    .Factory(clientModule)
                    .create(),
                safehillSyncManager = SafehillSyncManager(
                    backgroundTasksRegistry = clientModule.backgroundTasksRegistry,
                    userScope = clientModule.clientOptions.userScope
                ),
                userDataManager = UserDataManager
                    .Factory(clientModule)
                    .create()
            )
        }
    }
}