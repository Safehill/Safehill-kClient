package com.safehill.kclient.controllers

import com.safehill.kclient.models.dtos.InteractionsGroupDTO
import com.safehill.kclient.models.dtos.MessageInputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.network.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kcrypto.base64.base64EncodedString
import com.safehill.kcrypto.models.EncryptedData
import com.safehill.kcrypto.models.SymmetricKey
import java.util.Base64

class UserInteractionController(
    private val serverProxy: ServerProxy,
    private val currentUser: LocalUser,
) {

    suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return serverProxy.listThreads()
    }

    suspend fun retrieveLastMessage(
        threadId: String
    ): MessageOutputDTO? {
        return serverProxy.localServer.retrieveLastMessage(threadId)
    }

    suspend fun sendMessage(
        message: String,
        threadId: String
    ): Result<MessageOutputDTO> {
        return runCatching {
            val symmetricKey = getSymmetricKey(threadId) ?: return Result.failure(
                InteractionErrors.MissingE2EEDetails(threadId)
            )
            val encryptedMessage =
                EncryptedData(data = message.toByteArray(), symmetricKey).encryptedData
            val messageDTO = MessageInputDTO(
                encryptedMessage = encryptedMessage.base64EncodedString(),
                senderPublicSignature = currentUser.publicSignatureData.base64EncodedString(),
                inReplyToAssetGlobalIdentifier = null,
                inReplyToInteractionId = null
            )
            serverProxy.addMessages(listOf(messageDTO), threadId).first()
        }
    }


    suspend fun retrieveInteractions(
        before: String?,
        threadId: String,
        limit: Int
    ): Result<InteractionsGroupDTO> {
        return runCatching {
            serverProxy.retrieveInteractions(
                inGroupId = threadId,
                per = limit,
                page = 1,
                before = before
            )
        }
    }

    suspend fun retrieveLocalInteractions(
        threadId: String,
        limit: Int,
        before: String?
    ): Result<List<MessageOutputDTO>> {
        return runCatching {
            serverProxy.localServer.retrieveInteractions(
                inGroupId = threadId,
                per = limit,
                before = before,
                page = 1
            ).messages
        }
    }


    suspend fun setUpThread(withUsers: List<ServerUser>): ConversationThreadOutputDTO {
        val usersAndSelf = (withUsers + currentUser).distinctBy { it.identifier }

        val existingThread = serverProxy.retrieveThread(
            usersIdentifiers = usersAndSelf.map { it.identifier }
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

    private suspend fun getSymmetricKey(threadId: String): SymmetricKey? {
        val encryptionDetails = serverProxy.retrieveThread(threadId = threadId)
        return encryptionDetails?.getSymmetricKey(currentUser)
    }


    sealed class InteractionErrors(
        msg: String
    ) : Exception(msg) {

        class MissingE2EEDetails(val threadId: String) :
            InteractionErrors("The E2EE details for thread with id $threadId is not found.")

    }
}