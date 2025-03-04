package com.safehill.kclient.controllers

import com.safehill.kclient.base64.base64EncodedString
import com.safehill.kclient.models.EncryptedData
import com.safehill.kclient.models.SymmetricKey
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.InteractionsGroupDTO
import com.safehill.kclient.models.dtos.MessageInputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.ReactionInputDTO
import com.safehill.kclient.models.dtos.ReactionOutputDTO
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.dtos.RemoveReactionInputDTO
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.interactions.ReactionType
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.exceptions.SafehillError
import com.safehill.kclient.util.runCatchingPreservingCancellationException
import com.safehill.kclient.util.safeApiCall

/**
 * Obtain [UserInteractionController]'s instance from configured [com.safehill.SafehillClient]
 */

class UserInteractionController internal constructor(
    private val serverProxy: ServerProxy,
    private val userProvider: UserProvider,
    private val encryptionDetailsController: EncryptionDetailsController
) {

    suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return serverProxy.listThreads()
    }

    suspend fun retrieveLastMessage(
        anchorId: String
    ): MessageOutputDTO? {
        return serverProxy.localServer.retrieveLastMessage(anchorId)
    }

    suspend fun sendMessage(
        message: String,
        anchorId: String,
        interactionAnchor: InteractionAnchor
    ): Result<MessageOutputDTO> {
        return runCatching {
            val currentUser = userProvider.get()
            val symmetricKey =
                getSymmetricKey(anchorId = anchorId, interactionAnchor = interactionAnchor)
                    ?: throw InteractionErrors.MissingE2EEDetails(
                        anchorId = anchorId,
                        anchor = interactionAnchor
                    )
            val encryptedMessage =
                EncryptedData(data = message.toByteArray(), symmetricKey).encryptedData
            val messageDTO = MessageInputDTO(
                encryptedMessage = encryptedMessage.base64EncodedString(),
                senderPublicSignature = currentUser.publicSignatureData.base64EncodedString(),
                inReplyToAssetGlobalIdentifier = null,
                inReplyToInteractionId = null
            )
            serverProxy.addMessages(
                messages = listOf(messageDTO),
                anchorId = anchorId,
                interactionAnchor = interactionAnchor
            ).first()
        }
    }


    suspend fun retrieveInteractions(
        before: String?,
        anchorId: String,
        interactionAnchor: InteractionAnchor,
        limit: Int
    ): Result<InteractionsGroupDTO> {
        return safeApiCall {
            serverProxy.retrieveInteractions(
                anchorId = anchorId,
                interactionAnchor = interactionAnchor,
                per = limit,
                page = 1,
                before = before
            )
        }
    }

    suspend fun setUpThread(
        withUsers: List<ServerUser>,
        withPhoneNumbers: List<String>
    ): ConversationThreadOutputDTO {
        val currentUser = userProvider.get()
        val usersAndSelf = (withUsers + currentUser).distinctBy { it.identifier }

        val existingThread = serverProxy.retrieveThread(
            usersIdentifiers = usersAndSelf.map { it.identifier },
            phoneNumbers = withPhoneNumbers
        )

        return if (existingThread != null) {
            existingThread
        } else {
            val encryptionDetails = encryptionDetailsController.getRecipientEncryptionDetails(
                users = usersAndSelf,
                secretKey = SymmetricKey()
            )
            serverProxy.createOrUpdateThread(
                name = null,
                recipientsEncryptionDetails = encryptionDetails,
                phoneNumbers = withPhoneNumbers
            )
        }
    }


    suspend fun addReaction(
        reactionType: ReactionType,
        groupId: GroupId
    ): Result<ReactionOutputDTO> {
        return runCatchingPreservingCancellationException {
            serverProxy.addReactions(
                reactions = listOf(
                    ReactionInputDTO(
                        inReplyToInteractionId = null,
                        inReplyToAssetGlobalIdentifier = null,
                        reactionType = reactionType.toServerValue()
                    )
                ),
                toGroupId = groupId
            ).first()
        }
    }

    suspend fun removeReaction(
        reactionType: ReactionType,
        groupId: GroupId
    ): Result<Unit> {
        return runCatchingPreservingCancellationException {
            serverProxy.removeReaction(
                reaction = RemoveReactionInputDTO(
                    reactionType = reactionType.toServerValue(),
                    inReplyToInteractionId = null,
                    inReplyToAssetGlobalIdentifier = null
                ),
                fromGroupId = groupId
            )
        }
    }

    suspend fun getSymmetricKey(
        anchorId: String,
        interactionAnchor: InteractionAnchor
    ): SymmetricKey? {
        return runCatchingPreservingCancellationException {
            val currentUser = userProvider.get()
            val encryptionDetails: RecipientEncryptionDetailsDTO? = when (interactionAnchor) {
                InteractionAnchor.THREAD -> {
                    serverProxy.retrieveThread(threadId = anchorId)?.encryptionDetails
                }

                InteractionAnchor.GROUP -> {
                    serverProxy.retrieveGroupUserEncryptionDetails(groupId = anchorId)
                }
            }
            encryptionDetails?.getSymmetricKey(currentUser)
        }.getOrNull()
    }

    suspend fun deleteThread(threadId: String): Result<Unit> {
        return runCatchingPreservingCancellationException {
            serverProxy.deleteThread(threadId = threadId)
        }
    }

    suspend fun leaveThread(threadId: String): Result<Unit> {
        return runCatchingPreservingCancellationException {
            val currentUser = userProvider.get()
            updateThreadMembers(
                threadId = threadId,
                membersPublicIdentifierToRemove = listOf(currentUser.identifier)
            ).onSuccess {
                serverProxy.localServer.deleteThread(threadId = threadId)
            }.getOrThrow()
        }
    }

    suspend fun updateThreadMembers(
        threadId: String,
        usersToAdd: List<ServerUser> = listOf(),
        membersPublicIdentifierToRemove: List<UserIdentifier> = listOf(),
        phoneNumbersToAdd: List<String> = listOf(),
        phoneNumbersToRemove: List<String> = listOf()
    ): Result<Unit> {
        val currentThread: ConversationThreadOutputDTO
        return runCatchingPreservingCancellationException {
            val currentUser = userProvider.get()
            currentThread = serverProxy.retrieveThread(threadId = threadId)
                ?: throw InteractionErrors.ThreadNotFound(threadId)
            val symmetricKey = currentThread.encryptionDetails.getSymmetricKey(currentUser)
                ?: throw InteractionErrors.MissingE2EEDetails(
                    anchorId = threadId,
                    anchor = InteractionAnchor.THREAD
                )
            val encryptionDetails = encryptionDetailsController.getRecipientEncryptionDetails(
                users = usersToAdd,
                secretKey = symmetricKey
            )
            serverProxy.updateThreadMembers(
                threadId = threadId,
                recipientsToAdd = encryptionDetails,
                membersPublicIdentifierToRemove = membersPublicIdentifierToRemove,
                phoneNumbersToAdd = phoneNumbersToAdd,
                phoneNumbersToRemove = phoneNumbersToRemove
            )
        }.recoverCatching { exception ->
            if (exception is SafehillError.ClientError.Conflict) {
                with(currentThread) {
                    val newUserIdentifiers = membersPublicIdentifier.toSet()
                        .plus(usersToAdd.map { it.identifier })
                        .minus(membersPublicIdentifierToRemove.toSet())

                    val newPhoneNumbers = invitedUsersPhoneNumbers.keys
                        .plus(phoneNumbersToAdd)
                        .minus(phoneNumbersToRemove.toSet())

                    val conflictingThread = serverProxy.retrieveThread(
                        usersIdentifiers = newUserIdentifiers.toList(),
                        phoneNumbers = newPhoneNumbers.toList()
                    )
                    throw if (conflictingThread != null) {
                        InteractionErrors.ThreadConflict(conflictingThread = conflictingThread)
                    } else {
                        exception
                    }
                }
            } else {
                throw exception
            }
        }
    }

    sealed class InteractionErrors(
        msg: String
    ) : Exception(msg) {

        class MissingE2EEDetails(val anchorId: String, anchor: InteractionAnchor) :
            InteractionErrors("The E2EE details for anchor = $anchor with id $anchor is not found.")

        class ThreadConflict(val conflictingThread: ConversationThreadOutputDTO) :
            InteractionErrors("The thread with the given members already exists.")

        class ThreadNotFound(val threadId: String) :
            InteractionErrors("The given thread does not exist.")
    }
}