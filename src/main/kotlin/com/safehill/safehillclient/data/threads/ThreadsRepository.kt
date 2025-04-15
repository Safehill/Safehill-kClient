package com.safehill.safehillclient.data.threads

import com.safehill.kclient.controllers.UserInteractionController
import com.safehill.kclient.models.dtos.ConversationThreadAssetDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.InteractionsThreadSummaryDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.websockets.ThreadUpdatedDTO
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.tasks.outbound.UploadOperation
import com.safehill.kclient.tasks.outbound.UploadOperationListener
import com.safehill.kclient.tasks.syncing.InteractionSync
import com.safehill.kclient.tasks.syncing.InteractionSyncListener
import com.safehill.kclient.util.runCatchingSafe
import com.safehill.safehillclient.data.threads.factory.ThreadStateInteractorFactory
import com.safehill.safehillclient.data.threads.interactor.ThreadStateInteractor
import com.safehill.safehillclient.data.threads.model.Thread
import com.safehill.safehillclient.data.threads.model.ThreadState
import com.safehill.safehillclient.data.threads.registry.ThreadStateRegistry
import com.safehill.safehillclient.data.user.model.AppUser
import com.safehill.safehillclient.data.user.model.toServerUser
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.module.config.ClientOptions
import com.safehill.safehillclient.utils.extensions.createChildScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThreadsRepository(
    private val clientOptions: ClientOptions,
    private val userInteractionController: UserInteractionController,
    private val threadStateRegistry: ThreadStateRegistry,
    private val threadStateInteractorFactory: ThreadStateInteractorFactory,
    private val interactionSync: InteractionSync,
    private val uploadOperation: UploadOperation
) : UserObserver,
    InteractionSyncListener,
    UploadOperationListener {

    private val userScope = clientOptions.userScope

    private val safehillLogger = clientOptions.safehillLogger

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    val threads: StateFlow<List<Thread>> = threadStateRegistry
        .threadStates
        .map { it.values.toList() }
        .toThreads()
        .map { threads -> threads.sortedByDescending { it.lastActiveTime } }
        .stateIn(clientOptions.clientScope, SharingStarted.Eagerly, listOf())


    private fun syncThreadsWithServer(): Job {
        return clientOptions.userScope.launch {
            _loading.update { true }
            val threadDtos = userInteractionController.listThreads()
            threadStateRegistry.setThreadStates(threadDtos)
            _loading.update { false }
        }
    }


    fun getThreadState(threadId: String): Flow<ThreadState?> {
        return threadStateRegistry.threadStates.map { it[threadId] }
    }

    fun getThreadStateInteractor(threadId: String): ThreadStateInteractor? {
        val mutableThreadState = threadStateRegistry.getMutableThreadState(threadId) ?: return null
        return threadStateInteractorFactory.create(
            threadID = threadId,
            scope = clientOptions.userScope.createChildScope { SupervisorJob(it) },
            mutableThreadState = mutableThreadState
        )
    }

    private suspend fun setThreads(threadOutputDTO: List<ConversationThreadOutputDTO>) {
        val threadStates = threadStateRegistry.setThreadStates(threadOutputDTO)
        threadStates.forEach {
            getThreadStateInteractor(it.threadId)?.retrieveLastMessage()
        }
    }

    fun refreshThreads(): Job {
        return syncThreadsWithServer()
    }

    suspend fun setUpThread(
        withUsers: List<AppUser>,
        withPhoneNumbers: List<String>
    ): Result<ConversationThreadOutputDTO> {
        return withContext(Dispatchers.IO) {
            runCatchingSafe {
                userInteractionController.setUpThread(
                    withUsers = withUsers.map { it.toServerUser() },
                    withPhoneNumbers = withPhoneNumbers
                ).also {
                    threadStateRegistry.upsertThreadStates(listOf(it))
                }
            }
        }
    }

    suspend fun addThreadMembers(
        threadId: String,
        newUsers: List<ServerUser>,
        phoneNumbers: List<String>
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            userInteractionController.updateThreadMembers(
                threadId = threadId,
                usersToAdd = newUsers,
                phoneNumbersToAdd = phoneNumbers
            )
        }
    }

    suspend fun leaveThread(threadId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            userInteractionController.leaveThread(threadId = threadId)
                .onSuccess {
                    cleanUpThreadStates(threadId = threadId)
                }
        }
    }

    suspend fun deleteThread(threadId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            userInteractionController
                .deleteThread(threadId = threadId)
                .onSuccess {
                    cleanUpThreadStates(threadId = threadId)
                }
        }
    }

    private fun cleanUpThreadStates(threadId: String) {
        threadStateRegistry.deleteThreadState(threadId = threadId)
    }

    override suspend fun didUpdateThreadsList(threadDTOs: List<ConversationThreadOutputDTO>) {
        setThreads(threadDTOs)
    }

    override suspend fun didAddThread(threadDTO: ConversationThreadOutputDTO) {
        threadStateRegistry.upsertThreadStates(listOf(threadDTO))
    }

    override suspend fun didReceiveTextMessages(
        messageDtos: List<MessageOutputDTO>,
        anchorId: String,
        anchor: InteractionAnchor
    ) {
        if (anchor == InteractionAnchor.THREAD) {
            getThreadStateInteractor(anchorId)?.apply {
                upsertMessageDTO(messageDTOs = messageDtos)
                updateLastUpdatedTimeWithLatestMessage()
            }
        }
    }

    override suspend fun didUpdateThread(threadUpdatedDTO: ThreadUpdatedDTO) {
        getThreadStateInteractor(threadUpdatedDTO.threadId)?.update(
            threadUpdatedDTO = threadUpdatedDTO
        )
    }

    override suspend fun didReceivePhotoMessages(
        threadId: String,
        conversationThreadAssetDtos: List<ConversationThreadAssetDTO>
    ) {
        getThreadStateInteractor(threadId)?.let {
            it.upsertThreadAssetDTOs(conversationThreadAssetDtos)
            it.updateAssets()
        }
    }

    override suspend fun didFetchRemoteThreadSummary(summaryByThreadId: Map<String, InteractionsThreadSummaryDTO>) {
        coroutineScope {
            setThreads(threadOutputDTO = summaryByThreadId.map { it.value.thread })
            summaryByThreadId.map {
                async {
                    val threadId = it.key
                    getThreadStateInteractor(threadId)?.apply {
                        upsertMessageDTO(listOf(it.value.lastEncryptedMessage))
                        setTotalNumOfSharedPhotos(it.value.numAssets)
                    }
                }
            }.awaitAll()
        }
    }

    override suspend fun userLoggedIn(user: LocalUser) {
        syncThreadsWithServer().join()
        interactionSync.addListener(this)
        uploadOperation.listeners.add(this)
    }

    override fun userLoggedOut() {
        threadStateRegistry.clear()
        interactionSync.removeListener(this)
        uploadOperation.listeners.remove(this)
    }

}

@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<List<ThreadState>>.toThreads() = this
    .distinctUntilChanged()
    .flatMapLatest { threadStates ->
        val threads = threadStates.map { it.toThread() }
        // If threads is empty, the combine will never emit and no empty list of thread is emitted.
        // https://github.com/Kotlin/kotlinx.coroutines/issues/1603
        if (threads.isEmpty()) flowOf(emptyList()) else combine(threads) { it.toList() }
    }.distinctUntilChanged()