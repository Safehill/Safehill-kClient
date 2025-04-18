package com.safehill.kclient.network.api.thread

import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.CreateOrUpdateThreadDTO
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.dtos.RetrieveThreadDTO
import com.safehill.kclient.models.dtos.thread.ConversationThreadMembersUpdateDTO
import com.safehill.kclient.models.dtos.thread.ConversationThreadNameUpdateDTO
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.RequestMethod
import com.safehill.kclient.network.api.fireRequest
import com.safehill.kclient.network.api.postRequest
import com.safehill.kclient.network.api.postRequestForResponse
import com.safehill.kclient.network.exceptions.SafehillError

class ThreadApiImpl(
    baseApi: BaseApi
) : ThreadApi, BaseApi by baseApi {

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
        postRequest(
            endPoint = "/threads/update/$threadId",
            request = request
        )
    }

    private suspend fun listThreads(retrieveThreadDTO: RetrieveThreadDTO?): List<ConversationThreadOutputDTO> {
        return postRequestForResponse<RetrieveThreadDTO, List<ConversationThreadOutputDTO>>(
            endPoint = "/threads/retrieve",
            request = retrieveThreadDTO
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
            postRequestForResponse<Unit, ConversationThreadOutputDTO>(
                endPoint = "/threads/retrieve/$threadId",
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
        return postRequestForResponse(
            endPoint = "/threads/upsert",
            request = request
        )
    }

    override suspend fun convertInvitees(
        threadIdWithEncryptionDetails: Map<String, List<RecipientEncryptionDetailsDTO>>
    ) {
        val request = mapOf("newRecipientsByThreadId" to threadIdWithEncryptionDetails)
        postRequest(
            endPoint = "/threads/convert-invitees",
            request = request
        )
    }

    override suspend fun updateThreadMembers(
        threadId: String,
        recipientsToAdd: List<RecipientEncryptionDetailsDTO>,
        membersPublicIdentifierToRemove: List<UserIdentifier>,
        phoneNumbersToAdd: List<String>,
        phoneNumbersToRemove: List<String>
    ) {
        val request = ConversationThreadMembersUpdateDTO(
            recipientsToAdd = recipientsToAdd,
            membersPublicIdentifierToRemove = membersPublicIdentifierToRemove,
            phoneNumbersToAdd = phoneNumbersToAdd,
            phoneNumbersToRemove = phoneNumbersToRemove
        )
        postRequest(
            endPoint = "/threads/update/$threadId/members",
            request = request
        )
    }


    override suspend fun deleteThread(threadId: String) {
        fireRequest<Unit, String>(
            requestMethod = RequestMethod.Delete,
            endPoint = "/threads/$threadId",
            request = null
        )
    }


}