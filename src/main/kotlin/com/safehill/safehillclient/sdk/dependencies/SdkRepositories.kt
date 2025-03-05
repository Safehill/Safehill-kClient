package com.safehill.safehillclient.sdk.dependencies

import com.safehill.kclient.controllers.module.ControllersModule
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.UserProvider
import com.safehill.safehillclient.sdk.backgroundsync.BackgroundTasksRegistry
import com.safehill.safehillclient.sdk.backgroundsync.ClientOptions
import com.safehill.safehillclient.sdk.backgroundsync.NetworkModule
import com.safehill.safehillclient.sdk.data.authorization.UserAuthorizationRepository
import com.safehill.safehillclient.sdk.data.threads.ThreadsRepository
import com.safehill.safehillclient.sdk.data.threads.factory.RepositoriesFactory
import com.safehill.safehillclient.sdk.data.user_discovery.UserDiscoveryRepository
import com.safehill.safehillclient.sdk.module.sdk.SdkModule

class SdkRepositories private constructor(
    backgroundTaskRegistry: BackgroundTasksRegistry,
    clientOptions: ClientOptions,
    controllersModule: ControllersModule,
    userProvider: UserProvider,
    networkModule: NetworkModule
) : Repositories {

    private val repositoriesFactory: RepositoriesFactory by lazy {
        RepositoriesFactory(
            backgroundTasksRegistry = backgroundTaskRegistry,
            networkModule = networkModule,
            clientOptions = clientOptions,
            controllersModule = controllersModule,
            userProvider = userProvider
        )
    }

    override val userAuthorizationRepository: UserAuthorizationRepository by lazy {
        repositoriesFactory.createUserAuthorizationRepository()
    }

    override val threadsRepository: ThreadsRepository by lazy {
        repositoriesFactory.createThreadsRepository()
    }

    override val userDiscoveryRepository: UserDiscoveryRepository by lazy {
        repositoriesFactory.createUserDiscoveryRepository()
    }

    override fun userSet(user: LocalUser) {
        userAuthorizationRepository.userSet(user)
        threadsRepository.userSet(user)
    }

    override fun clearUser(clearPersistence: Boolean) {
        userAuthorizationRepository.clearUser(clearPersistence)
        threadsRepository.clearUser(clearPersistence)
    }

    class Factory {
        fun create(sdkModule: SdkModule): Repositories {
            return with(sdkModule) {
                SdkRepositories(
                    backgroundTaskRegistry = backgroundTasksRegistry,
                    clientOptions = clientOptions,
                    controllersModule = controllersModule,
                    userProvider = userProvider,
                    networkModule = networkModule
                )
            }
        }
    }
}