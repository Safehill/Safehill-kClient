package com.safehill.kclient.tasks.syncing

import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.websockets.InteractionSocketMessage
import com.safehill.kclient.models.dtos.websockets.TextMessage
import com.safehill.kclient.models.dtos.websockets.ThreadAssets
import com.safehill.kclient.models.dtos.websockets.ThreadCreated
import com.safehill.kclient.models.dtos.websockets.WebSocketMessage
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.WebSocketApi
import com.safehill.kclient.tasks.BackgroundTask
import com.safehill.kclient.util.runCatchingPreservingCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class InteractionSync(
    interactionSyncListeners: List<InteractionSyncListener>,
    private val serverProxy: ServerProxy,
    private val webSocketApi: WebSocketApi
) : BackgroundTask {

    private val interactionSyncListener =
        InteractionSyncListenerListDelegate(interactionSyncListeners)

    private val singleTaskExecutor = SingleTaskExecutor()

    override suspend fun run() {
        singleTaskExecutor.execute {
            coroutineScope {
                launch { syncThreadInteractionSummary() }
                launch {
                    webSocketApi.socketMessages.collect {
                        launch { it.handle() }
                    }
                }
            }
        }
    }

    private suspend fun WebSocketMessage.handle() {
        when (this) {
            is InteractionSocketMessage -> {
                when (this) {
                    is TextMessage -> {
                        this.notifyTextMessage()
                    }

                    is ThreadAssets -> {
                        this.notifyNewAssetInThread()
                    }

                    is ThreadCreated -> {
                        this.notifyCreationOfThread()
                    }
                }
            }

            else -> {}
        }
    }

    private suspend fun ThreadCreated.notifyCreationOfThread() {
        interactionSyncListener.didAddThread(
            threadDTO = thread
        ).also {
            serverProxy.localServer.createOrUpdateThread(
                threads = listOf(thread)
            )
        }
    }

    private suspend fun ThreadAssets.notifyNewAssetInThread() {
        interactionSyncListener.didReceivePhotoMessages(
            threadId = threadId,
            conversationThreadAssetDtos = assets
        )
    }

    private suspend fun TextMessage.notifyTextMessage() {
        interactionSyncListener.didReceiveTextMessages(
            messageDtos = listOf(this.toMessageDTO()),
            anchorId = this.anchorId,
            anchor = anchorType
        ).also {
            serverProxy.localServer.insertMessages(
                messages = listOf(this.toMessageDTO()),
                anchorId = this.anchorId
            )
        }
    }

    private fun TextMessage.toMessageDTO() = MessageOutputDTO(
        interactionId = interactionId,
        senderUserIdentifier = senderPublicIdentifier,
        inReplyToAssetGlobalIdentifier = inReplyToAssetGlobalIdentifier,
        encryptedMessage = encryptedMessage,
        createdAt = sentAt,
        inReplyToInteractionId = inReplyToInteractionId
    )

    private suspend fun syncThreadInteractionSummary() {
        coroutineScope {
            val interactionsSummary = runCatchingPreservingCancellationException {
                serverProxy.topLevelInteractionsSummary()
            }
            interactionsSummary
                .onSuccess { interactionsSummaryDTO ->
                    launch {
                        interactionSyncListener.didFetchRemoteGroupSummary(interactionsSummaryDTO.summaryByGroupId)
                    }
                    launch {
                        val allThreads =
                            interactionsSummaryDTO.summaryByThreadId.map { it.value.thread }
                        val localThreads = serverProxy.localServer.listThreads()
                        deleteThreadsNoLongerOnRemote(
                            allThreads = allThreads,
                            localThreads = localThreads
                        )
                    }
                    launch {
                        interactionSyncListener.didFetchRemoteThreadSummary(
                            interactionsSummaryDTO.summaryByThreadId
                        )
                    }
                }
        }
    }

    private suspend fun deleteThreadsNoLongerOnRemote(
        allThreads: List<ConversationThreadOutputDTO>,
        localThreads: List<ConversationThreadOutputDTO>
    ) {
        val threadIdsToRemoveLocally = localThreads.filter { localThread ->
            localThread.threadId !in allThreads.map { it.threadId }
        }.map { it.threadId }
        if (threadIdsToRemoveLocally.isNotEmpty()) {
            serverProxy.localServer.deleteThreads(threadIdsToRemoveLocally)
        }
    }
}