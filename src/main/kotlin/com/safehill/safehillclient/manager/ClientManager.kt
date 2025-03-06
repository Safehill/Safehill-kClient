package com.safehill.safehillclient.manager

import com.safehill.kclient.models.users.LocalUser
import com.safehill.safehillclient.backgroundsync.SafehillSyncManager
import com.safehill.safehillclient.manager.api.DeviceIdRegistrationHandler
import com.safehill.safehillclient.manager.api.SocketManager
import com.safehill.safehillclient.data.user.api.UserObserverRegistry
import com.safehill.safehillclient.manager.dependencies.SdkRepositories
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.module.client.ClientModule

class ClientManager(
    private val clientModule: ClientModule,
    private val sdkRepositoriesFactory: SdkRepositories.Factory,
    private val deviceRegistrationHandlerFactory: DeviceIdRegistrationHandler.Factory,
    private val socketManagerFactory: SocketManager.Factory
) : UserObserver {

    val repositories = sdkRepositoriesFactory.create()

    private val deviceIdRegistrationHandler = deviceRegistrationHandlerFactory.create()

    private val socketManager = socketManagerFactory.create()

    private val safehillSyncManager: SafehillSyncManager = SafehillSyncManager(
        backgroundTasksRegistry = clientModule.backgroundTasksRegistry,
        userScope = clientModule.clientOptions.userScope
    )

    private val observerRegistry = UserObserverRegistry().apply {
        addUserObserver(repositories)
        addUserObserver(deviceIdRegistrationHandler)
        addUserObserver(safehillSyncManager)
        addUserObserver(socketManager)
    }


    override suspend fun userSet(user: LocalUser) {
        clientModule.userSet(user)
        observerRegistry.userSet(user)
    }

    override fun clearUser(clearPersistence: Boolean) {
        observerRegistry.clearUser(clearPersistence)
        clientModule.clearUser(clearPersistence)
    }

    class Factory(
        private val clientModule: ClientModule
    ) {

        fun create(): ClientManager {
            return ClientManager(
                clientModule = clientModule,
                sdkRepositoriesFactory = SdkRepositories.Factory(clientModule),
                deviceRegistrationHandlerFactory = DeviceIdRegistrationHandler.Factory(clientModule),
                socketManagerFactory = SocketManager.Factory(clientModule)
            )
        }
    }
}