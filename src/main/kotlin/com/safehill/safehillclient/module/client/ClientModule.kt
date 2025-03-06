package com.safehill.safehillclient.module.client

import com.safehill.kclient.controllers.module.ControllersModule
import com.safehill.kclient.errors.LocalUserError
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.util.Provider
import com.safehill.safehillclient.backgroundsync.ClientOptions
import com.safehill.safehillclient.backgroundsync.SafehillBackgroundTasksRegistryFactory
import com.safehill.safehillclient.factory.NetworkModuleFactory
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.module.asset.AssetModule
import com.safehill.safehillclient.module.platform.PlatformModule
import com.safehill.safehillclient.module.platform.UserModule
import kotlinx.coroutines.CoroutineScope
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference

internal typealias UserScope = CoroutineScope

class UserObserverRegistry : UserObserver {
    private val userObservers = Collections.synchronizedList(mutableListOf<UserObserver>())

    fun addUserObserver(userObserver: UserObserver) {
        userObservers.add(userObserver)
    }

    fun removeUserObserver(userObserver: UserObserver) {
        userObservers.remove(userObserver)
    }

    override suspend fun userSet(user: LocalUser) {
        userObservers.forEach { it.userSet(user) }
    }

    override fun clearUser(clearPersistence: Boolean) {
        userObservers.forEach { it.clearUser(clearPersistence) }
    }
}


class ClientModule(
    networkModuleFactory: NetworkModuleFactory,
    val clientOptions: ClientOptions,
    val platformModule: PlatformModule,
    val userModule: UserModule
) : UserObserver {

    private val currentUser = AtomicReference<LocalUser?>(null)

    val userProvider = Provider {
        currentUser.get() ?: throw LocalUserError.UnAvailable()
    }

    val assetModule = AssetModule(
        platformModule = platformModule,
        userModule = userModule,
        userProvider = userProvider
    )

    val networkModule = networkModuleFactory.create(userProvider = userProvider)

    val controllersModule = ControllersModule(
        userProvider = userProvider,
        networkModule = networkModule,
        assetModule = assetModule
    )

    val backgroundTasksRegistry = SafehillBackgroundTasksRegistryFactory(
        assetModule = assetModule,
        networkModule = networkModule,
        userModule = userModule,
        userProvider = userProvider
    ).create()

    override suspend fun userSet(user: LocalUser) {
        currentUser.set(user)
    }

    override fun clearUser(clearPersistence: Boolean) {
        currentUser.set(null)
    }
}

val ClientModule.serverProxy
    get() = this.networkModule.serverProxy