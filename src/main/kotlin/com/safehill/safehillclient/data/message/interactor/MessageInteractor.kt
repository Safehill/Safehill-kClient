package com.safehill.safehillclient.data.message.interactor

import com.safehill.kclient.controllers.UserInteractionController
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.models.users.getOrNull
import com.safehill.kclient.util.runCatchingSafe
import com.safehill.safehillclient.data.message.model.Message
import com.safehill.safehillclient.data.message.model.MessageStatus
import com.safehill.safehillclient.data.message.model.MessageType
import com.safehill.safehillclient.data.message.model.MessagesContainer
import com.safehill.safehillclient.data.message.model.MutableMessagesContainer
import com.safehill.safehillclient.data.message.model.toMessage
import com.safehill.safehillclient.module.client.UserScope
import com.safehill.safehillclient.utils.extensions.errorMsg
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

interface MessageInteractor {

    val messagesContainer: MessagesContainer
    suspend fun sendMessage(message: String): Result<Message>
    suspend fun loadMessages(): Result<List<Message>>
    suspend fun retryMessage(message: Message): Result<Message>
    suspend fun retrieveLastMessage()
    fun loadMoreMessages()
    suspend fun upsertMessageDTO(messageDTOs: List<MessageOutputDTO?>): List<Message>
}

class MessageInteractorImpl(
    private val anchorId: String,
    private val interactionAnchor: InteractionAnchor,
    private val mutableMessagesContainer: MutableMessagesContainer,
    private val interactionController: UserInteractionController,
    private val userScope: UserScope,
    private val userProvider: UserProvider
) : MessageInteractor {

    override val messagesContainer: MessagesContainer = mutableMessagesContainer

    private suspend fun sendMessage(message: Message): Result<Message> {
        return runCatchingSafe {
            val currentUser = userProvider.get()
            mutableMessagesContainer.upsertMessage(message)
            mutableMessagesContainer.setLastUpdatedAt(message.createdDate)
            userScope.async {
                when (val messageType = message.messageType) {
                    is MessageType.Images -> {
                        error("Retrying of image not supported for now.")
                    }

                    is MessageType.Text -> {
                        interactionController.sendMessage(
                            anchorId = anchorId,
                            interactionAnchor = interactionAnchor,
                            message = messageType.message
                        ).fold(
                            onSuccess = { messageDTO ->
                                messageDTO.toMessage(
                                    decryptedMessage = messageType.message,
                                    userIdentifier = currentUser.identifier,
                                    status = MessageStatus.Sent
                                )
                            },
                            onFailure = {
                                message.copy(
                                    createdDate = Instant.now(),
                                    status = MessageStatus.Error(it.errorMsg)
                                )
                            }
                        ).also {
                            mutableMessagesContainer.updateMessage(message.id, it)
                        }
                    }
                }
            }.await()
        }
    }

    override suspend fun sendMessage(message: String): Result<Message> {
        return runCatchingSafe {
            val currentUser = userProvider.get()
            val temporaryChatMessage = Message(
                id = UUID.randomUUID().toString(),
                text = message.trim(),
                senderIdentifier = currentUser.identifier,
                userIdentifier = currentUser.identifier,
                createdDate = Instant.now(),
                status = MessageStatus.Sending,
            )
            sendMessage(message = temporaryChatMessage).getOrThrow()
        }
    }

    override suspend fun upsertMessageDTO(
        messageDTOs: List<MessageOutputDTO?>
    ): List<Message> {
        val symmetricKey = interactionController.getSymmetricKey(
            anchorId = anchorId, interactionAnchor = interactionAnchor
        ) ?: return listOf()
        val currentUser = userProvider.getOrNull() ?: return listOf()
        val messages = messageDTOs.filterNotNull().map { messageDTO ->
            messageDTO.toMessage(
                key = symmetricKey,
                currentUser = currentUser,
                status = MessageStatus.Sent
            )
        }
        mutableMessagesContainer.upsertMessages(messages)
        return messages
    }

    override suspend fun loadMessages(): Result<List<Message>> {
        val messageDtoResult = interactionController.retrieveInteractions(
            anchorId = anchorId,
            limit = 20,
            before = null,
            interactionAnchor = interactionAnchor
        )
        return messageDtoResult.mapCatching { messageDtos ->
            upsertMessageDTO(
                messageDTOs = messageDtos.messages
            )
        }
    }


    override suspend fun retryMessage(message: Message): Result<Message> {
        return sendMessage(
            message = message.copy(
                status = MessageStatus.Sending,
                createdDate = Instant.now()
            )
        )
    }


    override fun loadMoreMessages() {
        val lastMessage = messagesContainer
            .messages.value
            .lastOrNull { it.messageType is MessageType.Text } ?: return

        userScope.launch {
            messagesContainer.setIsLoadingMore(true)
            val messages = interactionController.retrieveInteractions(
                anchorId = anchorId,
                limit = EACH_PAGE_MESSAGE_SIZE,
                before = lastMessage.createdDate.toString(),
                interactionAnchor = interactionAnchor
            )
            messages
                .onSuccess {
                    messagesContainer.setEndOfContents(
                        it.messages.size < EACH_PAGE_MESSAGE_SIZE
                    )
                    upsertMessageDTO(it.messages)
                }.onFailure {
                    messagesContainer.setLoadMoreError(it)
                }
            messagesContainer.setIsLoadingMore(false)
        }
    }

    override suspend fun retrieveLastMessage() {
        userScope.launch {
            val lastMessage = interactionController.retrieveLastMessage(anchorId = anchorId)
            upsertMessageDTO(listOf(lastMessage))
        }
    }

    companion object {
        const val EACH_PAGE_MESSAGE_SIZE = 20
    }
}
