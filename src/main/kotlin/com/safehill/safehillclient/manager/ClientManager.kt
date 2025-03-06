package com.safehill.safehillclient.manager

import com.safehill.kclient.models.users.LocalUser
import com.safehill.safehillclient.backgroundsync.SafehillSyncManager
import com.safehill.safehillclient.dependencies.SdkRepositories
import com.safehill.safehillclient.dependencies.UserObserver
import com.safehill.safehillclient.module.sdk.ClientModule
import com.safehill.safehillclient.module.sdk.UserObserverRegistry

class ClientManager(
    private val clientModule: ClientModule
) : UserObserver {

    val repositories = SdkRepositories.Factory().create(clientModule)

    private val observerRegistry = UserObserverRegistry().apply {
        addUserObserver(repositories)
    }

    val safehillSyncManager: SafehillSyncManager = SafehillSyncManager(
        backgroundTasksRegistry = clientModule.backgroundTasksRegistry,
        userScope = clientModule.clientOptions.userScope
    )

    override fun userSet(user: LocalUser) {
        clientModule.userSet(user)
        observerRegistry.userSet(user)
    }

    override fun clearUser(clearPersistence: Boolean) {
        observerRegistry.clearUser(clearPersistence)
        clientModule.clearUser(clearPersistence)
    }
}