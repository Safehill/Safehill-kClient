package com.safehill.kclient.network.api.thread

import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.HashedPhoneNumber
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.users.UserIdentifier

interface ThreadApi {

    suspend fun listThreads(): List<ConversationThreadOutputDTO>

    suspend fun retrieveThread(
        threadId: String
    ): ConversationThreadOutputDTO?

    suspend fun retrieveThread(
        usersIdentifiers: List<UserIdentifier>,
        phoneNumbers: List<HashedPhoneNumber>
    ): ConversationThreadOutputDTO?


    suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
    ): ConversationThreadOutputDTO
}