package com.safehill.safehillclient.data.activity.repository

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.InteractionsGroupSummaryDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.tasks.syncing.InteractionSyncListener
import com.safehill.safehillclient.data.activity.controller.download.AssetsDownloadActivities
import com.safehill.safehillclient.data.activity.interactor.GroupInteractionsInteractor
import com.safehill.safehillclient.data.activity.interactor.GroupInteractionsInteractorFactory
import com.safehill.safehillclient.data.user.api.DefaultUserObserverRegistry
import com.safehill.safehillclient.data.user.api.UserObserverRegistry
import com.safehill.safehillclient.module.client.UserScope
import com.safehill.safehillclient.utils.extensions.createChildScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ActivityRepository(
    private val assetsDownloadActivities: AssetsDownloadActivities,
    private val groupInteractionsInteractorFactory: GroupInteractionsInteractorFactory,
    private val userScope: UserScope
) : InteractionSyncListener, UserObserverRegistry by DefaultUserObserverRegistry(
    assetsDownloadActivities
) {

    val downloadActivities = assetsDownloadActivities.downloads

    fun getGroupInteractor(groupId: GroupId): GroupInteractionsInteractor? {
        return downloadActivities.value[groupId]?.let {
            groupInteractionsInteractorFactory.create(
                groupId = groupId,
                abstractAssetActivity = it,
                scope = userScope.createChildScope { SupervisorJob(it) }
            )
        }
    }

    override suspend fun didReceiveTextMessages(
        messageDtos: List<MessageOutputDTO>,
        anchorId: String,
        anchor: InteractionAnchor
    ) {
        if (anchor == InteractionAnchor.GROUP) {
            getGroupInteractor(anchorId)?.apply {
                upsertMessageDTO(messageDtos)
                increaseCommentsCount(messageDtos.size)
            }
        }
    }

    override suspend fun didFetchRemoteGroupSummary(summaryByGroupId: Map<GroupId, InteractionsGroupSummaryDTO>) {
        coroutineScope {
            summaryByGroupId.forEach { (groupId, groupSummary) ->
                launch {
                    // Suspend till the group is available
                    downloadActivities.first { it[groupId] != null }
                    getGroupInteractor(groupId)?.updateGroupSummary(groupSummary)
                }
            }
        }
    }
}