package com.safehill.safehillclient.sdk.module.sdk

import com.safehill.kclient.controllers.module.ControllersModule
import com.safehill.kclient.errors.LocalUserError
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.util.Provider
import com.safehill.safehillclient.sdk.backgroundsync.ClientOptions
import com.safehill.safehillclient.sdk.backgroundsync.SafehillBackgroundTasksRegistryFactory
import com.safehill.safehillclient.sdk.backgroundsync.SafehillSyncManager
import com.safehill.safehillclient.sdk.dependencies.SdkRepositories
import com.safehill.safehillclient.sdk.dependencies.UserObserver
import com.safehill.safehillclient.sdk.factory.serverproxy.NetworkModuleFactory
import com.safehill.safehillclient.sdk.module.asset.AssetModule
import com.safehill.safehillclient.sdk.platform.PlatformModule
import com.safehill.safehillclient.sdk.platform.UserModule
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

    override fun userSet(user: LocalUser) {
        userObservers.forEach { it.userSet(user) }
    }

    override fun clearUser(clearPersistence: Boolean) {
        userObservers.forEach { it.clearUser(clearPersistence) }
    }
}


class ClientManager(
    private val sdkModule: SdkModule
) : UserObserver {

    val repositories = SdkRepositories.Factory().create(sdkModule)

    private val observerRegistry = UserObserverRegistry().apply {
        addUserObserver(repositories)
    }

    val safehillSyncManager: SafehillSyncManager = SafehillSyncManager(
        backgroundTasksRegistry = sdkModule.backgroundTasksRegistry,
        userScope = sdkModule.clientOptions.userScope
    )

    override fun userSet(user: LocalUser) {
        sdkModule.userSet(user)
        observerRegistry.userSet(user)
    }

    override fun clearUser(clearPersistence: Boolean) {
        observerRegistry.clearUser(clearPersistence)
        sdkModule.clearUser(clearPersistence)
    }
}

class SdkModule(
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

    override fun userSet(user: LocalUser) {
        currentUser.set(user)
    }

    override fun clearUser(clearPersistence: Boolean) {
        currentUser.set(null)
    }
}
