package com.safehill.kclient.network

import com.safehill.kclient.GlobalIdentifier
import com.safehill.kclient.api.SafehillApi
import com.safehill.kclient.api.dtos.SHMessageOutputDTO
import com.safehill.kclient.models.SHAssetDescriptor
import com.safehill.kclient.models.SHRemoteUser
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO

interface LocalServerInterface : SafehillApi {
    suspend fun createOrUpdateUser(
        identifier: String,
        name: String,
        publicKeyData: ByteArray,
        publicSignatureData: ByteArray
    )

    suspend fun createOrUpdateThread(threads: List<ConversationThreadOutputDTO>)

    suspend fun insertMessages(messages: List<SHMessageOutputDTO>, threadId: String)

    suspend fun retrieveLastMessage(threadId: String): SHMessageOutputDTO?

    suspend fun upsertUsers(remoteUsers: List<SHRemoteUser>)

    suspend fun deleteThreads(threadIds: List<String>)

    suspend fun getAssetDescriptors(
        globalIdentifiers: List<GlobalIdentifier>?,
        filteringGroups: List<String>?
    ): List<SHAssetDescriptor>

}

