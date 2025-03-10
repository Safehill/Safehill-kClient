package com.safehill.safehillclient.manager

import com.safehill.kclient.models.users.LocalUser
import com.safehill.safehillclient.backgroundsync.SafehillSyncManager
import com.safehill.safehillclient.data.user.api.UserObserverRegistry
import com.safehill.safehillclient.manager.api.DeviceIdRegistrationHandler
import com.safehill.safehillclient.manager.api.SocketManager
import com.safehill.safehillclient.manager.dependencies.Repositories
import com.safehill.safehillclient.manager.dependencies.SdkRepositories
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.module.client.ClientModule

class ClientManager(
    val repositories: Repositories,
    private val deviceRegistrationHandler: DeviceIdRegistrationHandler,
    private val socketManager: SocketManager,
    private val safehillSyncManager: SafehillSyncManager
) : UserObserver {

    private val observerRegistry = UserObserverRegistry().apply {
        addUserObserver(repositories)
        addUserObserver(deviceRegistrationHandler)
        addUserObserver(safehillSyncManager)
        addUserObserver(socketManager)
    }


    override suspend fun userSet(user: LocalUser) {
        observerRegistry.userSet(user)
    }

    override fun clearUser(clearPersistence: Boolean) {
        observerRegistry.clearUser(clearPersistence)
    }

    class Factory(
        private val clientModule: ClientModule
    ) {

        fun create(): ClientManager {
            return ClientManager(
                repositories = SdkRepositories
                    .Factory(clientModule)
                    .create(),
                deviceRegistrationHandler = DeviceIdRegistrationHandler
                    .Factory(clientModule)
                    .create(),
                socketManager = SocketManager
                    .Factory(clientModule)
                    .create(),
                safehillSyncManager = SafehillSyncManager(
                    backgroundTasksRegistry = clientModule.backgroundTasksRegistry,
                    userScope = clientModule.clientOptions.userScope
                )
            )
        }
    }
}