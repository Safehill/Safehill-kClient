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
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.interactions.ReactionType
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.util.runCatchingPreservingCancellationException
import com.safehill.kclient.util.safeApiCall
import java.util.Base64

/**
 * Obtain [UserInteractionController]'s instance from configured [com.safehill.SafehillClient]
 */

class UserInteractionController internal constructor(
    private val serverProxy: ServerProxy,
    private val currentUser: LocalUser,
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

    suspend fun setUpThread(withUsers: List<ServerUser>): ConversationThreadOutputDTO {
        val usersAndSelf = (withUsers + currentUser).distinctBy { it.identifier }

        val existingThread = serverProxy.retrieveThread(
            usersIdentifiers = usersAndSelf.map { it.identifier },
            phoneNumbers = listOf()
        )

        return if (existingThread != null) {
            existingThread
        } else {
            val encryptionDetails = getRecipientEncryptionDetails(
                usersAndSelf = usersAndSelf
            )
            serverProxy.createOrUpdateThread(
                name = null,
                recipientsEncryptionDetails = encryptionDetails
            )
        }
    }

    private fun getRecipientEncryptionDetails(
        usersAndSelf: List<ServerUser>
    ): List<RecipientEncryptionDetailsDTO> {
        val secretKey = SymmetricKey()
        return usersAndSelf.map { user ->
            val shareable = currentUser.shareable(
                data = secretKey.secretKeySpec.encoded,
                with = user,
                protocolSalt = currentUser.encryptionSalt
            )

            RecipientEncryptionDetailsDTO(
                recipientUserIdentifier = user.identifier,
                ephemeralPublicKey = Base64.getEncoder()
                    .encodeToString(shareable.ephemeralPublicKeyData),
                encryptedSecret = Base64.getEncoder().encodeToString(shareable.ciphertext),
                secretPublicSignature = Base64.getEncoder().encodeToString(shareable.signature),
                senderPublicSignature = Base64.getEncoder()
                    .encodeToString(currentUser.publicSignatureData)
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

    suspend fun getSymmetricKey(
        anchorId: String,
        interactionAnchor: InteractionAnchor
    ): SymmetricKey? {
        val encryptionDetails: RecipientEncryptionDetailsDTO? = when (interactionAnchor) {
            InteractionAnchor.THREAD -> {
                serverProxy.retrieveThread(threadId = anchorId)?.encryptionDetails
            }

            InteractionAnchor.GROUP -> {
                serverProxy.retrieveGroupUserEncryptionDetails(groupId = anchorId)
            }
        }
        return encryptionDetails?.getSymmetricKey(currentUser)
    }


    sealed class InteractionErrors(
        msg: String
    ) : Exception(msg) {

        class MissingE2EEDetails(val anchorId: String, anchor: InteractionAnchor) :
            InteractionErrors("The E2EE details for anchor = $anchor with id $anchor is not found.")

    }
}