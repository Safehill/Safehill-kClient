package com.safehill.safehillclient.data.threads.factory

import com.safehill.kclient.controllers.module.ControllersModule
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.network.ServerProxy
import com.safehill.safehillclient.backgroundsync.ClientOptions
import com.safehill.safehillclient.data.message.factory.MessageInteractorFactory
import com.safehill.safehillclient.data.threads.interactor.ThreadStateInteractor
import com.safehill.safehillclient.data.threads.model.MutableThreadState
import kotlinx.coroutines.CoroutineScope

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