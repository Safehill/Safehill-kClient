package com.safehill.kclient.network

import com.safehill.kclient.api.SafehillApi
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO

interface LocalServerInterface : SafehillApi {
    suspend fun createOrUpdateUser(
        identifier: String,
        name: String,
        publicKeyData: ByteArray,
        publicSignatureData: ByteArray
    )

    suspend fun createOrUpdateThread(threads: List<ConversationThreadOutputDTO>)

    suspend fun insertMessages(messages: List<MessageOutputDTO>, threadId: String)

    suspend fun retrieveLastMessage(threadId: String): MessageOutputDTO?

    suspend fun upsertUsers(remoteUsers: List<RemoteUser>)

    suspend fun deleteThreads(threadIds: List<String>)

}
