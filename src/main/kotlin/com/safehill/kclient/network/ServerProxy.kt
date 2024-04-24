package com.safehill.kclient.network

import com.safehill.kclient.api.AssetGlobalIdentifier
import com.safehill.kclient.api.SafehillApi
import com.safehill.kclient.api.dtos.HashedPhoneNumber
import com.safehill.kclient.api.dtos.SHAssetOutputDTO
import com.safehill.kclient.api.dtos.SHAuthResponseDTO
import com.safehill.kclient.api.dtos.SHInteractionsGroupDTO
import com.safehill.kclient.api.dtos.SHMessageInputDTO
import com.safehill.kclient.api.dtos.SHMessageOutputDTO
import com.safehill.kclient.api.dtos.SHReactionOutputDTO
import com.safehill.kclient.api.dtos.SHSendCodeToUserRequestDTO
import com.safehill.kclient.models.SHAssetDescriptor
import com.safehill.kclient.models.SHAssetDescriptorUploadState
import com.safehill.kclient.models.SHAssetQuality
import com.safehill.kclient.models.SHEncryptedAsset
import com.safehill.kclient.models.SHLocalUser
import com.safehill.kclient.models.SHRemoteUser
import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.models.SHShareableEncryptedAsset
import com.safehill.kclient.models.SHUserReaction
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.network.dtos.RecipientEncryptionDetailsDTO

class ServerProxy(
    val localServer: LocalServerInterface,
    val remoteServer: SafehillApi,
    override var requestor: SHLocalUser,
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

    override suspend fun getUsers(withIdentifiers: List<String>): List<SHRemoteUser> {
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

    override suspend fun getUsersWithPhoneNumber(hashedPhoneNumbers: List<HashedPhoneNumber>): Map<HashedPhoneNumber, SHRemoteUser> {
        TODO("Not yet implemented")
    }

    override suspend fun searchUsers(query: String, per: Int, page: Int): List<SHRemoteUser> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssetDescriptors(): List<SHAssetDescriptor> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssetDescriptors(assetGlobalIdentifiers: List<AssetGlobalIdentifier>): List<SHAssetDescriptor> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssets(
        globalIdentifiers: List<String>,
        versions: List<SHAssetQuality>?
    ): Map<String, SHEncryptedAsset> {
        TODO("Not yet implemented")
    }

    override suspend fun create(
        assets: List<SHEncryptedAsset>,
        groupId: String,
        filterVersions: List<SHAssetQuality>?
    ): List<SHAssetOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun share(asset: SHShareableEncryptedAsset) {
        TODO("Not yet implemented")
    }

    override suspend fun unshare(assetId: AssetGlobalIdentifier, userPublicIdentifier: String) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveThread(usersIdentifiers: List<String>): ConversationThreadOutputDTO? {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveThread(threadId: String): ConversationThreadOutputDTO? {
        return localServer.retrieveThread(threadId) ?: remoteServer.retrieveThread(threadId)?.also {
            localServer.insertThreads(listOf(it))
        }
    }

    override suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
    ): ConversationThreadOutputDTO {
        TODO("Not yet implemented")
    }

    override suspend fun upload(
        serverAsset: SHAssetOutputDTO,
        asset: SHEncryptedAsset,
        filterVersions: List<SHAssetQuality>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun markAsset(
        assetGlobalIdentifier: AssetGlobalIdentifier,
        quality: SHAssetQuality,
        asState: SHAssetDescriptorUploadState
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
        reactions: List<SHUserReaction>,
        toGroupId: String
    ): List<SHReactionOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun removeReaction(reaction: SHUserReaction, fromGroupId: String) {
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
    ): SHInteractionsGroupDTO {
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
    ): SHInteractionsGroupDTO {
        return remoteServer.retrieveInteractions(
            inGroupId = inGroupId,
            per = per,
            page = page,
            before = before
        )
    }

    override suspend fun addMessages(
        messages: List<SHMessageInputDTO>,
        threadId: String
    ): List<SHMessageOutputDTO> {
        return remoteServer.addMessages(messages, threadId).also {
            localServer.insertMessages(it, threadId)
        }
    }

    private suspend fun updateLocalUserDB(serverUsers: List<SHRemoteUser>): List<SHRemoteUser> {
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

    override suspend fun getAllLocalUsers(): List<SHServerUser> {
        TODO("Not yet implemented")
    }


    override suspend fun createUser(name: String): SHServerUser {
        TODO("Not yet implemented")
    }

    override suspend fun sendCodeToUser(
        countryCode: Int,
        phoneNumber: Long,
        code: String,
        medium: SHSendCodeToUserRequestDTO.Medium
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateUser(
        name: String?,
        phoneNumber: String?,
        email: String?
    ): SHServerUser {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount(name: String, password: String) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount() {
        TODO("Not yet implemented")
    }

    override suspend fun signIn(): SHAuthResponseDTO {
        TODO("Not yet implemented")
    }

}
