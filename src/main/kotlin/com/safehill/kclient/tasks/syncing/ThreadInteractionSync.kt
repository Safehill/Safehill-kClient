package com.safehill.kclient.tasks.syncing

import com.safehill.kclient.models.dtos.ConversationThreadAssetDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.InteractionsThreadSummaryDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.websockets.InteractionSocketMessage
import com.safehill.kclient.models.dtos.websockets.TextMessage
import com.safehill.kclient.models.dtos.websockets.ThreadAssets
import com.safehill.kclient.models.dtos.websockets.ThreadCreated
import com.safehill.kclient.models.dtos.websockets.WebSocketMessage
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.WebSocketApi
import com.safehill.kclient.tasks.BackgroundTask
import com.safehill.kclient.util.runCatchingPreservingCancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ThreadInteractionSync(
    private val serverProxy: ServerProxy,
    private val threadInteractionSyncListener: InteractionSyncListener,
    private val webSocketApi: WebSocketApi
) : BackgroundTask {

    override suspend fun run() {
        coroutineScope {
            syncThreadInteractionSummary()
            webSocketApi.socketMessages.collect {
                launch { it.handle() }
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
        threadInteractionSyncListener.didAddThread(
            threadDTO = thread
        ).also {
            serverProxy.localServer.createOrUpdateThread(
                threads = listOf(thread)
            )
        }
    }

    private suspend fun ThreadAssets.notifyNewAssetInThread() {
        threadInteractionSyncListener.didReceivePhotoMessages(
            threadId = threadId,
            conversationThreadAssetDtos = assets
        )
    }

    private suspend fun TextMessage.notifyTextMessage() {
        when (anchorType) {
            InteractionAnchor.THREAD -> {
                threadInteractionSyncListener.didReceiveTextMessages(
                    messageDtos = listOf(this.toMessageDTO()),
                    threadId = this.anchorId
                ).also {
                    serverProxy.localServer.insertMessages(
                        messages = listOf(this.toMessageDTO()),
                        threadId = this.anchorId
                    )
                }
            }

            InteractionAnchor.GROUP -> {}
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
            interactionsSummary.onSuccess { interactionsSummaryDTO ->
                val allThreads = interactionsSummaryDTO.summaryByThreadId.map {
                    it.value.thread
                }
                val localThreads = serverProxy.localServer.listThreads()
                deleteUnwantedThreadsLocally(
                    allThreads = allThreads,
                    localThreads = localThreads
                )
                launch {
                    threadInteractionSyncListener.didFetchRemoteThreadSummary(
                        interactionsSummaryDTO.summaryByThreadId
                    )
                }
            }
        }
    }

    private fun CoroutineScope.deleteUnwantedThreadsLocally(
        allThreads: List<ConversationThreadOutputDTO>,
        localThreads: List<ConversationThreadOutputDTO>
    ) {
        launch {
            val threadIdsToRemoveLocally = localThreads.filter { localThread ->
                localThread.threadId !in allThreads.map { it.threadId }
            }.map { it.threadId }
            if (threadIdsToRemoveLocally.isNotEmpty()) {
                serverProxy.localServer.deleteThreads(threadIdsToRemoveLocally)
            }
        }
    }
}

interface InteractionSyncListener {
    suspend fun didUpdateThreadsList(threadDTOs: List<ConversationThreadOutputDTO>)

    suspend fun didAddThread(threadDTO: ConversationThreadOutputDTO)

    suspend fun didReceiveTextMessages(messageDtos: List<MessageOutputDTO>, threadId: String)

    suspend fun didReceivePhotoMessages(
        threadId: String,
        conversationThreadAssetDtos: List<ConversationThreadAssetDTO>
    )

    suspend fun didFetchRemoteThreadSummary(summaryByThreadId: Map<String, InteractionsThreadSummaryDTO>)
}