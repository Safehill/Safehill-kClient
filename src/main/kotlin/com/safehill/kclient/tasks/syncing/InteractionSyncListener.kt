package com.safehill.kclient.tasks.syncing

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.ConversationThreadAssetDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.InteractionsGroupSummaryDTO
import com.safehill.kclient.models.dtos.InteractionsThreadSummaryDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.websockets.ThreadUpdatedDTO
import com.safehill.kclient.models.interactions.InteractionAnchor

interface InteractionSyncListener {
    suspend fun didUpdateThreadsList(threadDTOs: List<ConversationThreadOutputDTO>) {}

    suspend fun didAddThread(threadDTO: ConversationThreadOutputDTO) {}

    suspend fun didReceiveTextMessages(
        messageDtos: List<MessageOutputDTO>,
        anchorId: String,
        anchor: InteractionAnchor
    ) {
    }

    suspend fun didUpdateThread(threadUpdatedDTO: ThreadUpdatedDTO) {}

    suspend fun didReceivePhotoMessages(
        threadId: String,
        conversationThreadAssetDtos: List<ConversationThreadAssetDTO>
    ) {
    }

    suspend fun didFetchRemoteThreadSummary(summaryByThreadId: Map<String, InteractionsThreadSummaryDTO>) {}
    suspend fun didFetchRemoteGroupSummary(summaryByGroupId: Map<GroupId, InteractionsGroupSummaryDTO>) {}
}


class InteractionSyncListenerListDelegate
    : InteractionSyncListener,
    MutableList<InteractionSyncListener> by mutableListOf() {

    override suspend fun didUpdateThreadsList(threadDTOs: List<ConversationThreadOutputDTO>) {
        forEach { it.didUpdateThreadsList(threadDTOs) }
    }

    override suspend fun didAddThread(threadDTO: ConversationThreadOutputDTO) {
        forEach { it.didAddThread(threadDTO) }
    }

    override suspend fun didReceiveTextMessages(
        messageDtos: List<MessageOutputDTO>,
        anchorId: String,
        anchor: InteractionAnchor
    ) {
        forEach { it.didReceiveTextMessages(messageDtos, anchorId, anchor) }
    }

    override suspend fun didUpdateThread(threadUpdatedDTO: ThreadUpdatedDTO) {
        forEach {
            it.didUpdateThread(threadUpdatedDTO)
        }
    }

    override suspend fun didReceivePhotoMessages(
        threadId: String,
        conversationThreadAssetDtos: List<ConversationThreadAssetDTO>
    ) {
        forEach {
            it.didReceivePhotoMessages(
                threadId,
                conversationThreadAssetDtos
            )
        }
    }

    override suspend fun didFetchRemoteThreadSummary(summaryByThreadId: Map<String, InteractionsThreadSummaryDTO>) {
        forEach { it.didFetchRemoteThreadSummary(summaryByThreadId) }
    }

    override suspend fun didFetchRemoteGroupSummary(summaryByGroupId: Map<GroupId, InteractionsGroupSummaryDTO>) {
        forEach { it.didFetchRemoteGroupSummary(summaryByGroupId) }
    }
}