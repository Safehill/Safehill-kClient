package com.safehill.kclient.network.local

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.ConversationThreadAssetsDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.kclient.network.SafehillApi

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

    suspend fun getAssetDescriptors(
        globalIdentifiers: List<GlobalIdentifier>?,
        filteringGroups: List<String>?
    ): List<AssetDescriptor>

    suspend fun addThreadAssets(
        threadId: String,
        conversationThreadAssetsDTO: ConversationThreadAssetsDTO
    )

    fun create(
        assets: List<EncryptedAsset>,
        groupId: Map<GlobalIdentifier, AssetDescriptor>,
        filterVersions: AssetDescriptor.UploadState
    )

}
