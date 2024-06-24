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
import java.util.Date

class LocalServerInterfaceImpl : LocalServerInterface {
    override suspend fun createOrUpdateUser(
        identifier: String,
        name: String,
        publicKeyData: ByteArray,
        publicSignatureData: ByteArray,
    ) {
        error("Should not call this method")
    }

    override suspend fun createOrUpdateThread(threads: List<ConversationThreadOutputDTO>) {
        error("Should not call this method")
    }

    override suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>,
    ): ConversationThreadOutputDTO {
        error("Should not call this method")
    }

    override suspend fun insertMessages(messages: List<MessageOutputDTO>, threadId: String) {
        error("Should not call this method")
    }

    override suspend fun retrieveLastMessage(threadId: String): MessageOutputDTO? {
        error("Should not call this method")
    }

    override suspend fun upsertUsers(remoteUsers: List<RemoteUser>) {
        error("Should not call this method")
    }

    override suspend fun deleteThreads(threadIds: List<String>) {
        error("Should not call this method")
    }

    override suspend fun getAssetDescriptors(
        globalIdentifiers: List<GlobalIdentifier>?,
        filteringGroups: List<String>?
    ): List<AssetDescriptor> {
        error("Should not call this method")
    }

    override var requestor: LocalUser
        get() = error("Should not access requestor")
        set(value) {}

    override suspend fun createUser(name: String): ServerUser {
        error("Should not call this method")
    }

    override suspend fun sendCodeToUser(
        countryCode: Int,
        phoneNumber: Long,
        code: String,
        medium: SendCodeToUserRequestDTO.Medium,
    ) {
        error("Should not call this method")
    }

    override suspend fun updateUser(
        name: String?,
        phoneNumber: String?,
        email: String?
    ): ServerUser {
        error("Should not call this method")
    }

    override suspend fun deleteAccount() {
        error("Should not call this method")
    }

    override suspend fun signIn(): AuthResponseDTO {
        error("Should not call this method")
    }

    override suspend fun registerDevice(deviceId: String, token: String) {
        error("Should not call this method")
    }

    override suspend fun getUsers(withIdentifiers: List<UserIdentifier>): Map<UserIdentifier, RemoteUser> {
        error("Should not call this method")
    }

    override suspend fun getUsersWithPhoneNumber(hashedPhoneNumbers: List<HashedPhoneNumber>): Map<HashedPhoneNumber, RemoteUser> {
        error("Should not call this method")
    }

    override suspend fun searchUsers(query: String, per: Int, page: Int): List<RemoteUser> {
        error("Should not call this method")
    }

    override suspend fun getAssetDescriptors(after: Date?): List<AssetDescriptor> {
        error("Should not call this method")
    }

    override suspend fun addThreadAssets(
        threadId: String,
        conversationThreadAssetsDTO: ConversationThreadAssetsDTO
    ) {
        error("Should not call this method")
    }

    override suspend fun getAssets(threadId: String): ConversationThreadAssetsDTO {
        error("Should not call this method")
    }

    override suspend fun getAssetDescriptors(
        assetGlobalIdentifiers: List<AssetGlobalIdentifier>?,
        groupIds: List<GroupId>?,
        after: Date?
    ): List<AssetDescriptor> {
        error("Should not call this method")
    }

    override suspend fun getAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>?,
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        error("Should not call this method")
    }

    override suspend fun create(
        assets: List<EncryptedAsset>,
        groupId: GroupId,
        filterVersions: List<AssetQuality>?,
    ): List<AssetOutputDTO> {
        error("Should not call this method")
    }

    override suspend fun share(asset: ShareableEncryptedAsset) {
        error("Should not call this method")
    }

    override suspend fun unshare(
        assetId: AssetGlobalIdentifier,
        userPublicIdentifier: UserIdentifier
    ) {
        error("Should not call this method")
    }

    override suspend fun retrieveThread(usersIdentifiers: List<UserIdentifier>): ConversationThreadOutputDTO? {
        error("Should not call this method")
    }

    override suspend fun retrieveThread(threadId: String): ConversationThreadOutputDTO? {
        error("Should not call this method")
    }

    override suspend fun upload(
        serverAsset: AssetOutputDTO,
        asset: EncryptedAsset,
        filterVersions: List<AssetQuality>,
    ) {
        error("Should not call this method")
    }

    override suspend fun markAsset(
        assetGlobalIdentifier: AssetGlobalIdentifier,
        quality: AssetQuality,
        asState: AssetDescriptorUploadState,
    ) {
        error("Should not call this method")
    }

    override suspend fun deleteAssets(globalIdentifiers: List<AssetGlobalIdentifier>): List<AssetGlobalIdentifier> {
        error("Should not call this method")
    }

    override suspend fun setGroupEncryptionDetails(
        groupId: GroupId,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>,
    ) {
        error("Should not call this method")
    }

    override suspend fun deleteGroup(groupId: GroupId) {
        error("Should not call this method")
    }

    override suspend fun retrieveGroupUserEncryptionDetails(groupId: GroupId): List<RecipientEncryptionDetailsDTO> {
        error("Should not call this method")
    }

    override suspend fun addReactions(
        reactions: List<UserReactionDTO>,
        toGroupId: GroupId
    ): List<ReactionOutputDTO> {
        error("Should not call this method")
    }

    override suspend fun removeReaction(reaction: UserReactionDTO, fromGroupId: GroupId) {
        error("Should not call this method")
    }

    override suspend fun retrieveInteractions(
        inGroupId: GroupId,
        per: Int,
        page: Int,
        before: String?,
    ): InteractionsGroupDTO {
        error("Should not call this method")
    }

    override suspend fun addMessages(
        messages: List<MessageInputDTO>,
        groupId: GroupId
    ): List<MessageOutputDTO> {
        error("Should not call this method")
    }

    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        error("Should not call this method")
    }
}