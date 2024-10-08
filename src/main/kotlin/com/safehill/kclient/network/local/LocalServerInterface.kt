package com.safehill.kclient.network.local

import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.websockets.ThreadUpdatedDTO
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.network.SafehillApi

interface LocalServerInterface : SafehillApi, LocalAssetApi {
    suspend fun createOrUpdateUser(
        identifier: String,
        name: String,
        publicKeyData: ByteArray,
        publicSignatureData: ByteArray
    )

    suspend fun createOrUpdateThread(threads: List<ConversationThreadOutputDTO>)

    suspend fun updateThread(threadUpdatedDTO: ThreadUpdatedDTO)

    suspend fun insertMessages(messages: List<MessageOutputDTO>, anchorId: String)

    suspend fun retrieveLastMessage(anchorId: String): MessageOutputDTO?

    suspend fun upsertUsers(remoteUsers: List<RemoteUser>)

    suspend fun deleteThreads(threadIds: List<String>)

}
