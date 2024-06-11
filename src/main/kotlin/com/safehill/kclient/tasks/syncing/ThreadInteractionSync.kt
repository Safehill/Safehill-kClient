package com.safehill.kclient.tasks.syncing

import com.safehill.kclient.models.dtos.ConversationThreadAssetDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.websockets.ConnectionAck
import com.safehill.kclient.models.dtos.websockets.TextMessage
import com.safehill.kclient.models.dtos.websockets.ThreadAssets
import com.safehill.kclient.models.dtos.websockets.ThreadCreated
import com.safehill.kclient.models.dtos.websockets.UnknownMessage
import com.safehill.kclient.models.dtos.websockets.WebSocketMessage
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.WebSocketApi
import com.safehill.kclient.tasks.BackgroundTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

class ThreadInteractionSync(
    private val serverProxy: ServerProxy,
    private val threadInteractionSyncListener: InteractionSyncListener,
    private val webSocketApi: WebSocketApi,
    private val currentUser: LocalUser,
    private val deviceId: String
) : BackgroundTask {

    override suspend fun run() {
        coroutineScope {
            syncThreadInteractions()
            webSocketApi.connectToSocket(
                currentUser = currentUser,
                deviceId = deviceId,
            ) { socketData ->
                println("Socket Data: $socketData")
                launch {
                    socketData.handle()
                }
            }
        }

    }

    private suspend fun WebSocketMessage.handle() {
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

            UnknownMessage, is ConnectionAck -> {}
        }
    }

    private suspend fun ThreadCreated.notifyCreationOfThread() {
        threadInteractionSyncListener.didAddThread(
            threadDTO = thread
        )
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
                )
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

    private suspend fun syncThreadInteractions() {
        coroutineScope {
            val allThreads = serverProxy.listThreads()
            val localThreads = serverProxy.localServer.listThreads()

            deleteUnwantedThreadsLocally(
                allThreads = allThreads,
                localThreads = localThreads
            )

            val lastKnownThreadUpdateAt = localThreads.maxOfOrNull {
                it.lastUpdatedAt
            } ?: Instant.MIN

            allThreads.filter {
                val threadUpdateDate = it.lastUpdatedAt
                threadUpdateDate > lastKnownThreadUpdateAt
            }.forEach {
                updateThreadAndSyncInteractions(it)
            }
            threadInteractionSyncListener.didUpdateThreadsList(allThreads)
        }
    }

    private fun CoroutineScope.updateThreadAndSyncInteractions(threadOutputDTO: ConversationThreadOutputDTO) {
        launch {
            serverProxy.localServer.createOrUpdateThread(listOf(threadOutputDTO))

            val remoteInteractions = kotlin.runCatching {
                serverProxy.retrieveInteractions(
                    inGroupId = threadOutputDTO.threadId,
                    per = 20,
                    page = 1,
                    before = null
                )
            }
            remoteInteractions.onSuccess { interactionGroupDTO ->
                threadInteractionSyncListener.didReceiveTextMessages(
                    messageDtos = interactionGroupDTO.messages,
                    threadId = threadOutputDTO.threadId
                )
                serverProxy.localServer.insertMessages(
                    messages = interactionGroupDTO.messages,
                    threadId = threadOutputDTO.threadId
                )
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
}