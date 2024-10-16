package com.safehill.kclient.network.api.thread

import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.users.UserIdentifier

interface ThreadApi {

    suspend fun listThreads(): List<ConversationThreadOutputDTO>

    suspend fun updateThreadName(
        name: String?,
        threadId: String
    )

    suspend fun retrieveThread(
        threadId: String
    ): ConversationThreadOutputDTO?

    suspend fun retrieveThread(
        usersIdentifiers: List<UserIdentifier>,
        phoneNumbers: List<String>
    ): ConversationThreadOutputDTO?


    suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>,
        phoneNumbers: List<String>
    ): ConversationThreadOutputDTO

    suspend fun updateThreadMembers(
        threadId: String,
        recipientsToAdd: List<RecipientEncryptionDetailsDTO> = listOf(),
        membersPublicIdentifierToRemove: List<UserIdentifier> = listOf(),
        phoneNumbersToAdd: List<String> = listOf(),
        phoneNumbersToRemove: List<String> = listOf()
    )

    suspend fun deleteThread(
        threadId: String
    )
}