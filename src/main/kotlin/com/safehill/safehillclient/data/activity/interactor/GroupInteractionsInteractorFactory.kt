package com.safehill.safehillclient.data.activity.interactor

import com.safehill.kclient.controllers.UserInteractionController
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.UserProvider
import com.safehill.safehillclient.data.activity.model.DownloadRequest
import kotlinx.coroutines.CoroutineScope

class GroupInteractionsInteractorFactory(
    private val interactionController: UserInteractionController,
    private val userProvider: UserProvider
) {
    fun create(
        groupId: GroupId,
        abstractAssetActivity: DownloadRequest,
        scope: CoroutineScope
    ): GroupInteractionsInteractor? {
        return GroupInteractionsInteractor(
            groupId = groupId,
            abstractAssetActivity = abstractAssetActivity,
            interactionController = interactionController,
            userScope = scope,
            userProvider = userProvider
        )
    }
}