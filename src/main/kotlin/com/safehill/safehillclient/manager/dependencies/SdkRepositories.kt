package com.safehill.safehillclient.manager.dependencies

import com.safehill.kclient.controllers.module.ControllersModule
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.UserProvider
import com.safehill.safehillclient.backgroundsync.BackgroundTasksRegistry
import com.safehill.safehillclient.backgroundsync.NetworkModule
import com.safehill.safehillclient.data.authorization.UserAuthorizationRepository
import com.safehill.safehillclient.data.factory.RepositoriesFactory
import com.safehill.safehillclient.data.threads.ThreadsRepository
import com.safehill.safehillclient.data.user_discovery.UserDiscoveryRepository
import com.safehill.safehillclient.module.client.ClientModule
import com.safehill.safehillclient.module.config.ClientOptions

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

    override suspend fun userLoggedIn(user: LocalUser) {
        userAuthorizationRepository.userLoggedIn(user)
        threadsRepository.userLoggedIn(user)
    }

    override fun userLoggedOut() {
        userAuthorizationRepository.userLoggedOut()
        threadsRepository.userLoggedOut()
    }

    class Factory(
        private val clientModule: ClientModule
    ) {

        fun create(): Repositories {
            return with(clientModule) {
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