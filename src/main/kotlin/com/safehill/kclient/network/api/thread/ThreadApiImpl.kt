package com.safehill.kclient.network.api.thread

import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.CreateOrUpdateThreadDTO
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.dtos.RetrieveThreadDTO
import com.safehill.kclient.models.dtos.thread.ConversationThreadNameUpdateDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.postRequestForObjectResponse
import com.safehill.kclient.network.api.postRequestForStringResponse
import com.safehill.kclient.network.exceptions.SafehillError

class ThreadApiImpl(override val requestor: LocalUser) : ThreadApi, BaseApi {
    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return listThreads(null)
    }

    override suspend fun updateThreadName(
        name: String?,
        threadId: String
    ) {
        val request = ConversationThreadNameUpdateDTO(
            name = name
        )
         postRequestForStringResponse(
            endPoint = "/threads/update/$threadId",
            request = request,
            authenticationRequired = true
        )
    }

    private suspend fun listThreads(retrieveThreadDTO: RetrieveThreadDTO?): List<ConversationThreadOutputDTO> {
        return postRequestForObjectResponse<RetrieveThreadDTO, List<ConversationThreadOutputDTO>>(
            endPoint = "/threads/retrieve",
            request = retrieveThreadDTO,
            authenticationRequired = true
        )
    }

    override suspend fun retrieveThread(
        usersIdentifiers: List<UserIdentifier>,
        phoneNumbers: List<String>
    ): ConversationThreadOutputDTO? {
        return listThreads(
            RetrieveThreadDTO(
                byUsersPublicIdentifiers = usersIdentifiers,
                byInvitedPhoneNumbers = phoneNumbers
            )
        ).firstOrNull()
    }

    override suspend fun retrieveThread(threadId: String): ConversationThreadOutputDTO? {
        return runCatching {
            postRequestForObjectResponse<Unit, ConversationThreadOutputDTO>(
                endPoint = "/threads/retrieve/$threadId",
                authenticationRequired = true,
                request = null
            )
        }.recoverCatching {
            if (it is SafehillError.ClientError.NotFound) {
                null
            } else {
                throw it
            }
        }.getOrThrow()
    }

    override suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>,
        phoneNumbers: List<String>
    ): ConversationThreadOutputDTO {
        val request = CreateOrUpdateThreadDTO(
            name = name,
            recipients = recipientsEncryptionDetails,
            phoneNumbers = phoneNumbers
        )
        return postRequestForObjectResponse(
            endPoint = "/threads/upsert",
            request = request,
            authenticationRequired = true
        )
    }

}