package com.safehill.safehillclient.data.factory

import com.safehill.kclient.controllers.module.ControllersModule
import com.safehill.kclient.models.users.UserProvider
import com.safehill.safehillclient.backgroundsync.BackgroundTasksRegistry
import com.safehill.safehillclient.backgroundsync.NetworkModule
import com.safehill.safehillclient.data.activity.controller.download.AssetsDownloadActivities
import com.safehill.safehillclient.data.activity.interactor.GroupInteractionsInteractorFactory
import com.safehill.safehillclient.data.activity.repository.ActivityRepository
import com.safehill.safehillclient.data.authorization.UserAuthorizationRepository
import com.safehill.safehillclient.data.threads.ThreadsRepository
import com.safehill.safehillclient.data.threads.factory.ThreadStateInteractorFactory
import com.safehill.safehillclient.data.threads.registry.ThreadStateRegistry
import com.safehill.safehillclient.data.user_discovery.UserDiscoveryRepository
import com.safehill.safehillclient.module.asset.AssetModule
import com.safehill.safehillclient.module.config.ClientOptions

class RepositoriesFactory(
    private val networkModule: NetworkModule,
    private val clientOptions: ClientOptions,
    private val assetModule: AssetModule,
    private val backgroundTasksRegistry: BackgroundTasksRegistry,
    private val controllersModule: ControllersModule,
    private val userProvider: UserProvider,
) {

    fun createThreadsRepository(): ThreadsRepository {
        return ThreadsRepository(
            clientOptions = clientOptions,
            userInteractionController = controllersModule.interactionController,
            threadStateRegistry = ThreadStateRegistry(
                userController = controllersModule.userController,
                userProvider = userProvider
            ),
            threadStateInteractorFactory = ThreadStateInteractorFactory(
                userProvider = userProvider,
                controllersModule = controllersModule,
                clientOptions = clientOptions,
                serverProxy = networkModule.serverProxy
            ),
            interactionSync = backgroundTasksRegistry.interactionSync
        )
    }

    fun createUserAuthorizationRepository(): UserAuthorizationRepository {
        return UserAuthorizationRepository(
            userScope = clientOptions.userScope,
            serverProxy = networkModule.serverProxy,
            safehillLogger = clientOptions.safehillLogger,
            interactionSync = backgroundTasksRegistry.interactionSync,
            webSocketApi = networkModule.webSocketApi
        )
    }

    fun createUserDiscoveryRepository(): UserDiscoveryRepository {
        return UserDiscoveryRepository(
            serverProxy = networkModule.serverProxy,
            sdkDispatchers = clientOptions.sdkDispatchers
        )
    }

    fun createActivityRepository(): ActivityRepository {
        return ActivityRepository(
            assetsDownloadActivities = AssetsDownloadActivities(
                assetDescriptorsCache = assetModule.assetDescriptorCache,
                userController = controllersModule.userController,
                userScope = clientOptions.userScope
            ),
            groupInteractionsInteractorFactory = GroupInteractionsInteractorFactory(
                interactionController = controllersModule.interactionController,
                userProvider = userProvider
            ),
            userScope = clientOptions.userScope
        )
    }
}
