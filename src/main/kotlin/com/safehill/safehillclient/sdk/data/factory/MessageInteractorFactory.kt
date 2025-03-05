package com.safehill.safehillclient.sdk.data.factory

import com.safehill.kclient.controllers.UserInteractionController
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.users.UserProvider
import com.safehill.safehillclient.sdk.data.threads.interactor.MessageInteractorImpl
import com.safehill.safehillclient.sdk.data.threads.model.MutableMessagesContainer
import com.safehill.safehillclient.sdk.module.sdk.UserScope

class MessageInteractorFactory(
    private val interactionController: UserInteractionController,
    private val userScope: UserScope,
    private val userProvider: UserProvider
) {

    fun create(
        anchorId: String,
        interactionAnchor: InteractionAnchor,
        mutableMessagesContainer: MutableMessagesContainer
    ): MessageInteractorImpl {
        return MessageInteractorImpl(
            anchorId = anchorId,
            interactionAnchor = interactionAnchor,
            mutableMessagesContainer = mutableMessagesContainer,
            interactionController = interactionController,
            userScope = userScope,
            userProvider = userProvider
        )
    }
}