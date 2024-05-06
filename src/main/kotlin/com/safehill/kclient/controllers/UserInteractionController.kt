package com.safehill.kclient.controllers

import com.safehill.kclient.api.dtos.SHInteractionsGroupDTO
import com.safehill.kclient.api.dtos.SHMessageInputDTO
import com.safehill.kclient.api.dtos.SHMessageOutputDTO
import com.safehill.kclient.models.SHLocalUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO
import com.safehill.kcrypto.base64.base64EncodedString
import com.safehill.kcrypto.models.SHEncryptedData
import com.safehill.kcrypto.models.SHSymmetricKey

class UserInteractionController(
    private val serverProxy: ServerProxy,
    private val currentUser: SHLocalUser,
) {

    suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return serverProxy.listThreads()
    }

    suspend fun retrieveLastMessage(
        threadId: String
    ): SHMessageOutputDTO? {
        return serverProxy.localServer.retrieveLastMessage(threadId)
    }

    suspend fun sendMessage(
        message: String,
        threadId: String
    ): Result<SHMessageOutputDTO> {
        return runCatching {
            val symmetricKey = getSymmetricKey(threadId) ?: return Result.failure(
                InteractionErrors.MissingE2EEDetails(threadId)
            )
            val encryptedMessage =
                SHEncryptedData(data = message.toByteArray(), symmetricKey).encryptedData
            val messageDTO = SHMessageInputDTO(
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
    ): Result<SHInteractionsGroupDTO> {
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
    ): Result<List<SHMessageOutputDTO>> {
        return runCatching {
            serverProxy.localServer.retrieveInteractions(
                inGroupId = threadId,
                per = limit,
                before = before,
                page = 1
            ).messages
        }
    }


    private suspend fun getSymmetricKey(threadId: String): SHSymmetricKey? {
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