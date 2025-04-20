package com.safehill.safehillclient.module.client

import com.safehill.kclient.controllers.module.ControllersModule
import com.safehill.kclient.errors.LocalUserError
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.util.Provider
import com.safehill.safehillclient.backgroundsync.SafehillBackgroundTasksRegistryFactory
import com.safehill.safehillclient.factory.NetworkModuleFactory
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.module.asset.AssetModule
import com.safehill.safehillclient.module.config.ClientOptions
import com.safehill.safehillclient.module.config.Configs
import com.safehill.safehillclient.module.platform.PlatformModule
import com.safehill.safehillclient.module.platform.UserModule
import com.safehill.safehillclient.utils.extensions.cancelChildren
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.atomic.AtomicReference

internal typealias UserScope = CoroutineScope

class ClientModule(
    networkModuleFactory: NetworkModuleFactory,
    val configs: Configs,
    val clientOptions: ClientOptions,
    val platformModule: PlatformModule,
    val userModule: UserModule
) : UserObserver {

    private val currentUser = AtomicReference<LocalUser?>(null)

    val userProvider = Provider {
        currentUser.get() ?: throw LocalUserError.UnAvailable()
    }

    val networkModule = networkModuleFactory.create(userProvider = userProvider)

    val assetModule = AssetModule(
        platformModule = platformModule,
    )

    val controllersModule = ControllersModule(
        userProvider = userProvider,
        networkModule = networkModule,
        assetModule = assetModule
    )

    val backgroundTasksRegistry = SafehillBackgroundTasksRegistryFactory(
        assetModule = assetModule,
        networkModule = networkModule,
        userModule = userModule,
        userProvider = userProvider,
        controllersModule = controllersModule
    ).create()

    override suspend fun userLoggedIn(user: LocalUser) {
        currentUser.set(user)
    }

    override fun userLoggedOut() {
        assetModule.assetDescriptorCache.clearAssetDescriptors()
        clientOptions.userScope.cancelChildren()
        currentUser.set(null)
    }
}