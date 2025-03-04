package com.safehill.kclient.tasks.syncing

import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.websockets.InteractionSocketMessage
import com.safehill.kclient.models.dtos.websockets.TextMessage
import com.safehill.kclient.models.dtos.websockets.ThreadAssets
import com.safehill.kclient.models.dtos.websockets.ThreadCreated
import com.safehill.kclient.models.dtos.websockets.ThreadUpdatedDTO
import com.safehill.kclient.models.dtos.websockets.WebSocketMessage
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.WebSocketApi
import com.safehill.kclient.tasks.BackgroundTask
import com.safehill.kclient.util.runCatchingSafe
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class InteractionSync(
    private val serverProxy: ServerProxy,
    private val webSocketApi: WebSocketApi
) : BackgroundTask {

    private val interactionSyncListeners = InteractionSyncListenerListDelegate()

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

                    is ThreadUpdatedDTO -> {
                        this.handleThreadUpdate()
                    }
                }
            }

            else -> {}
        }
    }

    private suspend fun ThreadUpdatedDTO.handleThreadUpdate() {
        serverProxy.localServer.updateThread(this)
        interactionSyncListeners.didUpdateThread(this)
    }

    private suspend fun ThreadCreated.notifyCreationOfThread() {
        interactionSyncListeners.didAddThread(
            threadDTO = thread
        ).also {
            serverProxy.localServer.createOrUpdateThread(
                threads = listOf(thread)
            )
        }
    }

    private suspend fun ThreadAssets.notifyNewAssetInThread() {
        interactionSyncListeners.didReceivePhotoMessages(
            threadId = threadId,
            conversationThreadAssetDtos = assets
        )
    }

    private suspend fun TextMessage.notifyTextMessage() {
        interactionSyncListeners.didReceiveTextMessages(
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
            val interactionsSummary = runCatchingSafe {
                serverProxy.topLevelInteractionsSummary()
            }
            interactionsSummary
                .onSuccess { interactionsSummaryDTO ->
                    launch {
                        interactionSyncListeners.didFetchRemoteGroupSummary(interactionsSummaryDTO.summaryByGroupId)
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
                        interactionSyncListeners.didFetchRemoteThreadSummary(
                            interactionsSummaryDTO.summaryByThreadId
                        )
                    }
                }
        }
    }

    fun addListener(listener: InteractionSyncListener) {
        synchronized(interactionSyncListeners) {
            interactionSyncListeners.add(listener)
        }
    }

    fun removeListener(listener: InteractionSyncListener) {
        synchronized(interactionSyncListeners) {
            interactionSyncListeners.remove(listener)
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
            threadIdsToRemoveLocally.forEach { threadId ->
                serverProxy.localServer.deleteThread(threadId)
            }
        }
    }
}