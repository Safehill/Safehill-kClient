package com.safehill.kclient.network.local

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetDescriptorUploadState
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.ShareableEncryptedAsset
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.dtos.ConversationThreadAssetsDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.HashedPhoneNumber
import com.safehill.kclient.models.dtos.InteractionsGroupDTO
import com.safehill.kclient.models.dtos.InteractionsSummaryDTO
import com.safehill.kclient.models.dtos.MessageInputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.ReactionOutputDTO
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.dtos.SendCodeToUserRequestDTO
import com.safehill.kclient.models.dtos.UserReactionDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.kclient.network.exceptions.SafehillError
import com.safehill.kcrypto.models.LocalCryptoUser
import java.util.Date

class LocalServerInterfaceImpl : LocalServerInterface {
    override suspend fun createOrUpdateUser(
        identifier: String,
        name: String,
        publicKeyData: ByteArray,
        publicSignatureData: ByteArray,
    ) {
        throw SafehillError.ServerError.UnSupportedOperation
    }

    override suspend fun createOrUpdateThread(threads: List<ConversationThreadOutputDTO>) {
        TODO("Not yet implemented")
    }

    override suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>,
    ): ConversationThreadOutputDTO {
        TODO("Not yet implemented")
    }

    override suspend fun insertMessages(messages: List<MessageOutputDTO>, threadId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveLastMessage(threadId: String): MessageOutputDTO? {
        TODO("Not yet implemented")
    }

    override suspend fun upsertUsers(remoteUsers: List<RemoteUser>) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteThreads(threadIds: List<String>) {
        TODO("Not yet implemented")
    }

    override suspend fun getAssetDescriptors(
        globalIdentifiers: List<GlobalIdentifier>?,
        filteringGroups: List<String>?
    ): List<AssetDescriptor> {
        TODO("Not yet implemented")
    }

    override var requestor: LocalUser
        get() = LocalUser(LocalCryptoUser())
        set(value) {}

    override suspend fun createUser(name: String): ServerUser {
        throw SafehillError.ServerError.UnSupportedOperation
    }

    override suspend fun sendCodeToUser(
        countryCode: Int,
        phoneNumber: Long,
        code: String,
        medium: SendCodeToUserRequestDTO.Medium,
    ) {
        throw SafehillError.ServerError.UnSupportedOperation
    }

    override suspend fun updateUser(
        name: String?,
        phoneNumber: String?,
        email: String?
    ): ServerUser {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount() {
        TODO("Not yet implemented")
    }

    override suspend fun signIn(): AuthResponseDTO {
        throw SafehillError.ServerError.UnSupportedOperation
    }

    override suspend fun registerDevice(deviceId: String, token: String) {
        throw SafehillError.ServerError.UnSupportedOperation
    }

    override suspend fun getUsers(withIdentifiers: List<UserIdentifier>): Map<UserIdentifier, RemoteUser> {
        TODO("Not yet implemented")
    }

    override suspend fun getUsersWithPhoneNumber(hashedPhoneNumbers: List<HashedPhoneNumber>): Map<HashedPhoneNumber, RemoteUser> {
        TODO("Not yet implemented")
    }

    override suspend fun searchUsers(query: String, per: Int, page: Int): List<RemoteUser> {
        throw SafehillError.ServerError.UnSupportedOperation
    }

    override suspend fun getAssetDescriptors(after: Date?): List<AssetDescriptor> {
        TODO("Not yet implemented")
    }

    override suspend fun addThreadAssets(
        threadId: String,
        conversationThreadAssetsDTO: ConversationThreadAssetsDTO
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getAssets(threadId: String): ConversationThreadAssetsDTO {
        TODO("Not yet implemented")
    }

    override suspend fun getAssetDescriptors(
        assetGlobalIdentifiers: List<AssetGlobalIdentifier>?,
        groupIds: List<GroupId>?,
        after: Date?
    ): List<AssetDescriptor> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>?,
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        TODO("Not yet implemented")
    }

    override suspend fun create(
        assets: List<EncryptedAsset>,
        groupId: GroupId,
        filterVersions: List<AssetQuality>?,
    ): List<AssetOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun share(asset: ShareableEncryptedAsset) {
        TODO("Not yet implemented")
    }

    override suspend fun unshare(
        assetId: AssetGlobalIdentifier,
        userPublicIdentifier: UserIdentifier
    ) {
    }

    override suspend fun topLevelInteractionsSummary(): InteractionsSummaryDTO {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveThread(usersIdentifiers: List<UserIdentifier>): ConversationThreadOutputDTO? {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveThread(threadId: String): ConversationThreadOutputDTO? {
        TODO("Not yet implemented")
    }

    override suspend fun upload(
        serverAsset: AssetOutputDTO,
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

    override suspend fun deleteAssets(globalIdentifiers: List<AssetGlobalIdentifier>): List<AssetGlobalIdentifier> {
        TODO("Not yet implemented")
    }

    override suspend fun setGroupEncryptionDetails(
        groupId: GroupId,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteGroup(groupId: GroupId) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveGroupUserEncryptionDetails(groupId: GroupId): List<RecipientEncryptionDetailsDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun addReactions(
        reactions: List<UserReactionDTO>,
        toGroupId: GroupId
    ): List<ReactionOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun removeReaction(reaction: UserReactionDTO, fromGroupId: GroupId) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveInteractions(
        inGroupId: GroupId,
        per: Int,
        page: Int,
        before: String?
    ): InteractionsGroupDTO {
        TODO("Not yet implemented")
    }

    override suspend fun addMessages(
        messages: List<MessageInputDTO>,
        groupId: GroupId
    ): List<MessageOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        TODO("Not yet implemented")
    }
}