package com.safehill.kclient.tasks.syncing

import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
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
        syncThreadInteractions()
        webSocketApi.connectToSocket(
            currentUser = currentUser,
            deviceId = deviceId
        )
    }

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
}