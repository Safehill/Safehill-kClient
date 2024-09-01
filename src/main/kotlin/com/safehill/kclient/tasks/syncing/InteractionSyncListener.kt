package com.safehill.kclient.tasks.syncing

import com.safehill.kclient.models.dtos.ConversationThreadAssetDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.InteractionsThreadSummaryDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
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

    suspend fun didReceivePhotoMessages(
        threadId: String,
        conversationThreadAssetDtos: List<ConversationThreadAssetDTO>
    ) {
    }

    suspend fun didFetchRemoteThreadSummary(summaryByThreadId: Map<String, InteractionsThreadSummaryDTO>) {}
}


class InteractionSyncListenerListDelegate(
    private val interactionListeners: List<InteractionSyncListener>
) : InteractionSyncListener {
    override suspend fun didUpdateThreadsList(threadDTOs: List<ConversationThreadOutputDTO>) {
        interactionListeners.forEach { it.didUpdateThreadsList(threadDTOs) }
    }

    override suspend fun didAddThread(threadDTO: ConversationThreadOutputDTO) {
        interactionListeners.forEach { it.didAddThread(threadDTO) }
    }

    override suspend fun didReceiveTextMessages(
        messageDtos: List<MessageOutputDTO>,
        anchorId: String,
        anchor: InteractionAnchor
    ) {
        interactionListeners.forEach { it.didReceiveTextMessages(messageDtos, anchorId, anchor) }
    }

    override suspend fun didReceivePhotoMessages(
        threadId: String,
        conversationThreadAssetDtos: List<ConversationThreadAssetDTO>
    ) {
        interactionListeners.forEach {
            it.didReceivePhotoMessages(
                threadId,
                conversationThreadAssetDtos
            )
        }
    }

    override suspend fun didFetchRemoteThreadSummary(summaryByThreadId: Map<String, InteractionsThreadSummaryDTO>) {
        interactionListeners.forEach { it.didFetchRemoteThreadSummary(summaryByThreadId) }
    }

}