package com.safehill.kclient.network

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetDescriptorUploadState
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.ShareableEncryptedAsset
import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.dtos.ConversationThreadAssetDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.HashedPhoneNumber
import com.safehill.kclient.models.dtos.InteractionsGroupDTO
import com.safehill.kclient.models.dtos.MessageInputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.ReactionOutputDTO
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.dtos.SendCodeToUserRequestDTO
import com.safehill.kclient.models.interactions.UserReaction
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.ServerUser

class ServerProxy(
    val localServer: LocalServerInterface,
    val remoteServer: SafehillApi,
    override var requestor: LocalUser,
) : ServerProxyInterface {


    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return try {
            val remoteResult = remoteServer.listThreads()
            remoteResult
        } catch (error: Exception) {
            println("failed to fetch threads from server. Returning local version. ${error.localizedMessage}")
            localServer.listThreads()
        }
    }

    override suspend fun getUsers(withIdentifiers: List<String>): List<RemoteUser> {
        if (withIdentifiers.isEmpty()) {
            return emptyList()
        }

        return try {
            val remoteUsers = remoteServer.getUsers(withIdentifiers)
            val updateResult = updateLocalUserDB(serverUsers = remoteUsers)
            updateResult
        } catch (exception: Exception) {
            val localUsers = localServer.getUsers(withIdentifiers)
            if (localUsers.size == withIdentifiers.size) {
                localUsers
            } else {
                throw exception
            }
        }
    }

    override suspend fun getUsersWithPhoneNumber(hashedPhoneNumbers: List<HashedPhoneNumber>): Map<HashedPhoneNumber, RemoteUser> {
        TODO("Not yet implemented")
    }

    override suspend fun searchUsers(query: String, per: Int, page: Int): List<RemoteUser> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssetDescriptors(): List<AssetDescriptor> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssetDescriptors(assetGlobalIdentifiers: List<AssetGlobalIdentifier>): List<AssetDescriptor> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssets(threadId: String): List<ConversationThreadAssetDTO> {
        return localServer.getAssets(threadId = threadId).ifEmpty {
            remoteServer.getAssets(threadId = threadId)
        }
    }

    override suspend fun getAssets(
        globalIdentifiers: List<String>,
        versions: List<AssetQuality>?
    ): Map<String, EncryptedAsset> {
        TODO("Not yet implemented")
    }

    override suspend fun create(
        assets: List<EncryptedAsset>,
        groupId: String,
        filterVersions: List<AssetQuality>?
    ): List<com.safehill.kclient.models.dtos.AssetOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun share(asset: ShareableEncryptedAsset) {
        TODO("Not yet implemented")
    }

    override suspend fun unshare(assetId: AssetGlobalIdentifier, userPublicIdentifier: String) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveThread(usersIdentifiers: List<String>): ConversationThreadOutputDTO? {
        return localServer.retrieveThread(usersIdentifiers) ?: remoteServer.retrieveThread(
            usersIdentifiers
        )?.also {
            localServer.createOrUpdateThread(listOf(it))
        }
    }

    override suspend fun retrieveThread(threadId: String): ConversationThreadOutputDTO? {
        return localServer.retrieveThread(threadId) ?: remoteServer.retrieveThread(threadId)?.also {
            localServer.createOrUpdateThread(listOf(it))
        }
    }

    override suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
    ): ConversationThreadOutputDTO {
        return remoteServer.createOrUpdateThread(
            name = name,
            recipientsEncryptionDetails = recipientsEncryptionDetails
        ).also {
            localServer.createOrUpdateThread(listOf(it))
        }
    }

    override suspend fun upload(
        serverAsset: com.safehill.kclient.models.dtos.AssetOutputDTO,
        asset: EncryptedAsset,
        filterVersions: List<AssetQuality>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun markAsset(
        assetGlobalIdentifier: AssetGlobalIdentifier,
        quality: AssetQuality,
        asState: AssetDescriptorUploadState
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAssets(globalIdentifiers: List<String>): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun setGroupEncryptionDetails(
        groupId: String,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteGroup(groupId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveGroupUserEncryptionDetails(groupId: String): List<RecipientEncryptionDetailsDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun addReactions(
        reactions: List<UserReaction>,
        toGroupId: String
    ): List<ReactionOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun removeReaction(reaction: UserReaction, fromGroupId: String) {
        TODO("Not yet implemented")
    }

    /***
     * This function will try to fetch interactions from remote server and if it fails, retrieves locally.
     */
    override suspend fun retrieveInteractions(
        inGroupId: String,
        per: Int,
        page: Int,
        before: String?
    ): InteractionsGroupDTO {
        return try {
            retrieveRemoteInteractions(
                inGroupId = inGroupId,
                per = per,
                page = page,
                before = before
            )
        } catch (e: Exception) {
            println("failed to fetch interactions from server. Returning local version. $e ${e.message}")
            localServer.retrieveInteractions(
                inGroupId = inGroupId,
                per = per,
                page = page,
                before = before
            )
        }
    }

    suspend fun retrieveRemoteInteractions(
        inGroupId: String,
        per: Int,
        page: Int,
        before: String?
    ): InteractionsGroupDTO {
        return remoteServer.retrieveInteractions(
            inGroupId = inGroupId,
            per = per,
            page = page,
            before = before
        )
    }

    override suspend fun addMessages(
        messages: List<MessageInputDTO>,
        groupId: String
    ): List<MessageOutputDTO> {
        return remoteServer.addMessages(messages, groupId).also {
            localServer.insertMessages(it, groupId)
        }
    }

    private suspend fun updateLocalUserDB(serverUsers: List<RemoteUser>): List<RemoteUser> {
        serverUsers.forEach { serverUser ->
            try {
                localServer.createOrUpdateUser(
                    identifier = serverUser.identifier,
                    name = serverUser.name,
                    publicKeyData = serverUser.publicKeyData,
                    publicSignatureData = serverUser.publicSignatureData
                )
            } catch (exception: Exception) {
                println("failed to create server user in local server: $exception ${exception.message}")
            }
        }

        return localServer.getUsers(serverUsers.map { it.identifier })
    }

    override suspend fun getAllLocalUsers(): List<ServerUser> {
        TODO("Not yet implemented")
    }


    override suspend fun createUser(name: String): ServerUser {
        TODO("Not yet implemented")
    }

    override suspend fun sendCodeToUser(
        countryCode: Int,
        phoneNumber: Long,
        code: String,
        medium: SendCodeToUserRequestDTO.Medium
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateUser(
        name: String?,
        phoneNumber: String?,
        email: String?
    ): ServerUser {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount(name: String, password: String) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount() {
        TODO("Not yet implemented")
    }

    override suspend fun signIn(): AuthResponseDTO {
        TODO("Not yet implemented")
    }

}
