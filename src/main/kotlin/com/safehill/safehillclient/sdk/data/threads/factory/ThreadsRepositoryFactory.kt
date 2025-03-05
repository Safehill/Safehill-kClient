package com.safehill.safehillclient.sdk.data.threads.factory

import com.safehill.kclient.controllers.module.ControllersModule
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.network.ServerProxy
import com.safehill.safehillclient.sdk.backgroundsync.BackgroundTasksRegistry
import com.safehill.safehillclient.sdk.backgroundsync.ClientOptions
import com.safehill.safehillclient.sdk.backgroundsync.NetworkModule
import com.safehill.safehillclient.sdk.data.authorization.UserAuthorizationRepository
import com.safehill.safehillclient.sdk.data.factory.MessageInteractorFactory
import com.safehill.safehillclient.sdk.data.threads.ThreadsRepository
import com.safehill.safehillclient.sdk.data.threads.interactor.ThreadStateInteractor
import com.safehill.safehillclient.sdk.data.threads.model.MutableThreadState
import com.safehill.safehillclient.sdk.data.threads.registry.ThreadStateRegistry
import com.safehill.safehillclient.sdk.data.user_discovery.UserDiscoveryRepository
import kotlinx.coroutines.CoroutineScope

class RepositoriesFactory(
    private val networkModule: NetworkModule,
    private val clientOptions: ClientOptions,
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
}

class ThreadStateInteractorFactory(
    private val serverProxy: ServerProxy,
    private val controllersModule: ControllersModule,
    private val userProvider: UserProvider,
    private val clientOptions: ClientOptions
) {
    fun create(
        threadID: String,
        scope: CoroutineScope,
        mutableThreadState: MutableThreadState
    ): ThreadStateInteractor {
        return ThreadStateInteractor(
            threadId = threadID,
            scope = scope,
            mutableThreadState = mutableThreadState,
            serverProxy = serverProxy,
            userController = controllersModule.userController,
            messageInteractorFactory = MessageInteractorFactory(
                interactionController = controllersModule.interactionController,
                userScope = scope,
                userProvider = userProvider
            ),
            userProvider = userProvider,
            safehillLogger = clientOptions.safehillLogger
        )
    }
}