package com.safehill.safehillclient.data.threads.interactor

import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.ConversationThreadAssetDTO
import com.safehill.kclient.models.dtos.ConversationThreadAssetsDTO
import com.safehill.kclient.models.dtos.websockets.ThreadUpdatedDTO
import com.safehill.kclient.models.dtos.websockets.ThreadUpdatedDTO.Companion.toUpdatedDTO
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.models.users.getOrNull
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.util.safeApiCall
import com.safehill.safehillclient.data.message.factory.MessageInteractorFactory
import com.safehill.safehillclient.data.message.interactor.MessageInteractor
import com.safehill.safehillclient.data.message.model.Message
import com.safehill.safehillclient.data.message.model.MessageImageState
import com.safehill.safehillclient.data.message.model.MessageStatus
import com.safehill.safehillclient.data.message.model.MessageType
import com.safehill.safehillclient.data.threads.model.MutableThreadState
import com.safehill.safehillclient.data.user.model.toAppUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

class ThreadStateInteractor(
    private val threadId: String,
    private val scope: CoroutineScope,
    private val mutableThreadState: MutableThreadState,
    private val serverProxy: ServerProxy,
    private val userController: UserController,
    private val userProvider: UserProvider,
    private val safehillLogger: SafehillLogger,
    messageInteractorFactory: MessageInteractorFactory,
) : MessageInteractor by messageInteractorFactory.create(
    anchorId = threadId,
    interactionAnchor = InteractionAnchor.THREAD,
    mutableMessagesContainer = mutableThreadState
) {

    fun refresh() {
        scope.launch {
            val threadDTO = serverProxy.retrieveThread(threadId)
            if (threadDTO != null) {
                update(threadDTO.toUpdatedDTO())
            }
        }
    }

    fun updateAssets() {
        scope.launch {
            val assets = safeApiCall {
                serverProxy.getAssets(threadId = threadId)
            }
            assets
                .onSuccess { conversationThreadAssetsDTO ->
                    setThreadAssetDTOs(conversationThreadAssetsDTO)
                    setTotalNumOfSharedPhotos(conversationThreadAssetsDTO.photoMessages.size)
                }.onFailure {
                    safehillLogger.error("Error fetching asset of the thread $threadId")
                }
        }
    }

    suspend fun updateThreadName(name: String?): Result<Unit> {
        return safeApiCall {
            serverProxy.updateThreadName(
                name = name,
                threadId = threadId
            )
        }.onSuccess {
            mutableThreadState.setName(name = name)
        }
    }

    private fun setThreadAssetDTOs(threadAssetsDTO: ConversationThreadAssetsDTO) {
        mutableThreadState.setAssetIdentifiers(
            threadAssetsDTO.otherAssets.map { it.globalIdentifier } + threadAssetsDTO.photoMessages.map { it.globalIdentifier }
        )
        upsertPhotoMessages(threadAssetsDTO.photoMessages)
    }

    fun upsertThreadAssetDTOs(threadAssetDtos: List<ConversationThreadAssetDTO>) {
        mutableThreadState.upsertAssetIdentifiers(
            threadAssetDtos.map { it.globalIdentifier }
        )
        upsertPhotoMessages(threadAssetDtos = threadAssetDtos)
    }


    private fun ConversationThreadAssetDTO.toMessage(currentUser: LocalUser): Message {
        return Message(
            id = this.groupId,
            userIdentifier = currentUser.identifier,
            senderIdentifier = this.addedByUserIdentifier,
            createdDate = this.addedAt,
            status = MessageStatus.Sent,
            messageType = MessageType.Images(
                groupId = this.groupId,
                messageImageStates = listOf(
                    MessageImageState.Completed(this.globalIdentifier)
                )
            )
        )
    }

    fun setLastUpdatedAt(lastUpdatedAt: Instant) {
        mutableThreadState.setLastUpdatedAt(lastUpdatedAt)
    }

    fun setTotalNumOfSharedPhotos(count: Int) {
        mutableThreadState.setTotalNumOfAssets(count)
    }

    fun update(threadUpdatedDTO: ThreadUpdatedDTO) {
        scope.launch {
            val users = userController
                .getUsers(threadUpdatedDTO.membersPublicIdentifier)
                .map { usersMap -> usersMap.values.map { it.toAppUser() } }

            mutableThreadState.update(
                name = threadUpdatedDTO.name,
                lastUpdatedAt = threadUpdatedDTO.lastUpdatedAt,
                users = users.getOrNull() ?: mutableThreadState.users.value,
                invitedPhoneNumbers = threadUpdatedDTO.invitedUsersPhoneNumbers
            )
        }
    }

    fun updateLastUpdatedTimeWithLatestMessage() {
        mutableThreadState.messages.value.lastOrNull()?.createdDate?.let { lastActiveDate ->
            setLastUpdatedAt(lastActiveDate)
        }
    }

    private fun upsertPhotoMessages(threadAssetDtos: List<ConversationThreadAssetDTO>) {
        val currentUser = userProvider.getOrNull() ?: return
        threadAssetDtos.forEach { threadAsset ->
            upsertImageMessage(
                groupId = threadAsset.groupId,
                imageState = MessageImageState.Completed(threadAsset.globalIdentifier),
                createNew = { threadAsset.toMessage(currentUser) }
            )
        }
    }

    fun updateImageStatus(groupId: GroupId, imageState: MessageImageState) {
        upsertImageMessage(
            groupId = groupId,
            imageState = imageState,
            createNew = {
                Message(
                    id = groupId,
                    userIdentifier = userProvider.get().identifier,
                    senderIdentifier = userProvider.get().identifier,
                    createdDate = Instant.now(),
                    status = MessageStatus.Sent,
                    messageType = MessageType.Images(
                        groupId = groupId,
                        messageImageStates = listOf(imageState)
                    )
                )
            }
        )
    }

    private fun upsertImageMessage(
        groupId: String,
        imageState: MessageImageState,
        createNew: () -> Message
    ) {
        mutableThreadState.modifyMessages { messages ->
            val existingMessage = messages.values.lastOrNull {
                it.messageType is MessageType.Images && it.messageType.groupId == groupId
            }

            val updatedMessage = if (existingMessage != null) {
                val images = existingMessage.messageType as MessageType.Images
                existingMessage.copy(messageType = images.updateImageStatus(imageState))
            } else {
                createNew()
            }

            messages + (updatedMessage.id to updatedMessage)
        }
    }

}
