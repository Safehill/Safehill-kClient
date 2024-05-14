package com.safehill.kclient.network

import com.safehill.kclient.models.assets.AssetDescriptorUploadState
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.ShareableEncryptedAsset
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.HashedPhoneNumber
import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.dtos.InteractionsGroupDTO
import com.safehill.kclient.models.dtos.MessageInputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.ReactionOutputDTO
import com.safehill.kclient.models.dtos.SendCodeToUserRequestDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.dtos.UserReactionDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.local.LocalServerInterface
import java.util.Date

class ServerProxyImpl(
    override val localServer: LocalServerInterface,
    override val remoteServer: SafehillApi,
    override var requestor: LocalUser,
) : ServerProxy {


    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return try {
            val remoteResult = remoteServer.listThreads()
            remoteResult
        } catch (error: Exception) {
            println("failed to fetch threads from server. Returning local version. ${error.localizedMessage}")
            localServer.listThreads()
        }
    }

    override suspend fun getUsers(withIdentifiers: List<UserIdentifier>): Map<UserIdentifier, RemoteUser> {
        if (withIdentifiers.isEmpty()) {
            return emptyMap()
        }

        return try {
            val remoteUsers = remoteServer.getUsers(withIdentifiers)
            val updateResult = updateLocalUserDB(serverUsers = remoteUsers.values)
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
        return remoteServer.getUsersWithPhoneNumber(hashedPhoneNumbers)
    }

    override suspend fun searchUsers(query: String, per: Int, page: Int): List<RemoteUser> {
        return remoteServer.searchUsers(query, per, page)
    }

    override suspend fun getAssetDescriptors(after: Date?): List<AssetDescriptor> {
        return remoteServer.getAssetDescriptors(after)
    }

    override suspend fun getAssetDescriptors(
        assetGlobalIdentifiers: List<AssetGlobalIdentifier>?,
        groupIds: List<GroupId>?,
        after: Date?
    ): List<AssetDescriptor> {
        return remoteServer.getAssetDescriptors(assetGlobalIdentifiers, groupIds, after)
    }

    override suspend fun getAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>?
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        return remoteServer.getAssets(globalIdentifiers, versions)
    }

    override suspend fun create(
        assets: List<EncryptedAsset>,
        groupId: GroupId,
        filterVersions: List<AssetQuality>?
    ): List<AssetOutputDTO> {
        return remoteServer.create(assets, groupId, filterVersions)
    }

    override suspend fun share(asset: ShareableEncryptedAsset) {
        remoteServer.share(asset)
    }

    override suspend fun unshare(assetId: AssetGlobalIdentifier, userPublicIdentifier: UserIdentifier) {
        remoteServer.unshare(assetId, userPublicIdentifier)
    }

    override suspend fun retrieveThread(usersIdentifiers: List<UserIdentifier>): ConversationThreadOutputDTO? {
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
        serverAsset: AssetOutputDTO,
        asset: EncryptedAsset,
        filterVersions: List<AssetQuality>
    ) {
        remoteServer.upload(serverAsset, asset, filterVersions)
    }

    override suspend fun markAsset(
        assetGlobalIdentifier: AssetGlobalIdentifier,
        quality: AssetQuality,
        asState: AssetDescriptorUploadState
    ) {
        remoteServer.markAsset(assetGlobalIdentifier, quality, asState)
    }

    override suspend fun deleteAssets(globalIdentifiers: List<AssetGlobalIdentifier>): List<AssetGlobalIdentifier> {
        return remoteServer.deleteAssets(globalIdentifiers)
    }

    override suspend fun setGroupEncryptionDetails(
        groupId: GroupId,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
    ) {
        return remoteServer.setGroupEncryptionDetails(groupId, recipientsEncryptionDetails)
    }

    override suspend fun deleteGroup(groupId: GroupId) {
        remoteServer.deleteGroup(groupId)
    }

    override suspend fun retrieveGroupUserEncryptionDetails(groupId: GroupId): List<RecipientEncryptionDetailsDTO> {
        return remoteServer.retrieveGroupUserEncryptionDetails(groupId)
    }

    override suspend fun addReactions(
        reactions: List<UserReactionDTO>,
        toGroupId: GroupId
    ): List<ReactionOutputDTO> {
        return remoteServer.addReactions(reactions, toGroupId)
    }

    override suspend fun removeReaction(reaction: UserReactionDTO, fromGroupId: GroupId) {
        remoteServer.removeReaction(reaction, fromGroupId)
    }

    /***
     * This function will try to fetch interactions from remote server and if it fails, retrieves locally.
     */
    override suspend fun retrieveInteractions(
        inGroupId: GroupId,
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
        inGroupId: GroupId,
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
        groupId: GroupId
    ): List<MessageOutputDTO> {
        return remoteServer.addMessages(messages, groupId).also {
            localServer.insertMessages(it, groupId)
        }
    }

    private suspend fun updateLocalUserDB(serverUsers: Collection<RemoteUser>): Map<UserIdentifier, RemoteUser> {
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

    override suspend fun createUser(name: String): ServerUser {
        return remoteServer.createUser(name)
    }

    override suspend fun sendCodeToUser(
        countryCode: Int,
        phoneNumber: Long,
        code: String,
        medium: SendCodeToUserRequestDTO.Medium
    ) {
        return remoteServer.sendCodeToUser(countryCode, phoneNumber, code, medium)
    }

    override suspend fun updateUser(
        name: String?,
        phoneNumber: String?,
        email: String?
    ): ServerUser {
        return remoteServer.updateUser(name, phoneNumber, email)
    }

    override suspend fun deleteAccount() {
        remoteServer.deleteAccount()
    }

    override suspend fun signIn(): AuthResponseDTO {
        return remoteServer.signIn()
    }


    override suspend fun getAllLocalUsers(): List<ServerUser> {
        TODO("Not yet implemented")
    }
}
