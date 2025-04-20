package com.safehill.kclient.network.local

import com.safehill.kclient.models.LocalCryptoUser
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.ShareableEncryptedAsset
import com.safehill.kclient.models.dtos.ConversationThreadAssetsDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.HashedPhoneNumber
import com.safehill.kclient.models.dtos.InteractionsGroupDTO
import com.safehill.kclient.models.dtos.InteractionsSummaryDTO
import com.safehill.kclient.models.dtos.MessageInputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.ReactionInputDTO
import com.safehill.kclient.models.dtos.ReactionOutputDTO
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.dtos.RemoveReactionInputDTO
import com.safehill.kclient.models.dtos.SendCodeToUserRequestDTO
import com.safehill.kclient.models.dtos.authorization.UserAuthorizationStatusDTO
import com.safehill.kclient.models.dtos.websockets.ThreadUpdatedDTO
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.exceptions.SafehillError
import java.time.Instant

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

    override suspend fun updateThread(threadUpdatedDTO: ThreadUpdatedDTO) {
        TODO("Not yet implemented")
    }

    override suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>,
        phoneNumbers: List<String>
    ): ConversationThreadOutputDTO {
        TODO("Not yet implemented")
    }

    override suspend fun updateThreadMembers(
        threadId: String,
        recipientsToAdd: List<RecipientEncryptionDetailsDTO>,
        membersPublicIdentifierToRemove: List<UserIdentifier>,
        phoneNumbersToAdd: List<String>,
        phoneNumbersToRemove: List<String>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteThread(threadId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun convertInvitees(threadIdWithEncryptionDetails: Map<String, List<RecipientEncryptionDetailsDTO>>) {
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

    override suspend fun clear() {
        TODO("Not yet implemented")
    }

    override var requestor: LocalUser
        get() = LocalUser(LocalCryptoUser())
        set(value) {}

    override suspend fun storeAssetDescriptor(assetDescriptor: AssetDescriptor) {
        TODO("Not yet implemented")
    }

    override suspend fun storeAssetsWithDescriptor(encryptedAssetsWithDescriptor: Map<AssetDescriptor, EncryptedAsset>) {
        TODO("Not yet implemented")
    }

    override suspend fun storeThreadAssets(
        threadId: String,
        conversationThreadAssetsDTO: ConversationThreadAssetsDTO
    ) {
        TODO("Not yet implemented")
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

    override suspend fun registerDevice(deviceId: String, token: String?) {
        TODO("Not yet implemented")
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

    override suspend fun getAssetDescriptors(after: Instant?): List<AssetDescriptor> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssets(threadId: String): ConversationThreadAssetsDTO {
        TODO("Not yet implemented")
    }

    override suspend fun getEncryptedAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        TODO("Not yet implemented")
    }

    override suspend fun getAssetDescriptors(
        assetGlobalIdentifiers: List<AssetGlobalIdentifier>?,
        groupIds: List<GroupId>?,
        after: Instant?
    ): List<AssetDescriptor> {
        TODO("Not yet implemented")
    }


    override suspend fun share(asset: ShareableEncryptedAsset, threadId: String) {
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

    override suspend fun retrieveThread(
        usersIdentifiers: List<UserIdentifier>,
        phoneNumbers: List<HashedPhoneNumber>
    ): ConversationThreadOutputDTO? {
        TODO("Not yet implemented")
    }


    override suspend fun retrieveThread(threadId: String): ConversationThreadOutputDTO? {
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
        throw SafehillError.ServerError.NotImplemented
    }

    override suspend fun retrieveGroupUserEncryptionDetails(groupId: GroupId): RecipientEncryptionDetailsDTO {
        throw SafehillError.ServerError.NotImplemented
    }

    override suspend fun addReactions(
        reactions: List<ReactionInputDTO>,
        toGroupId: GroupId
    ): List<ReactionOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun removeReaction(reaction: RemoveReactionInputDTO, fromGroupId: GroupId) {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveInteractions(
        anchorId: String,
        interactionAnchor: InteractionAnchor,
        per: Int,
        page: Int,
        before: String?
    ): InteractionsGroupDTO {
        TODO("Not yet implemented")
    }

    override suspend fun addMessages(
        messages: List<MessageInputDTO>,
        anchorId: String,
        interactionAnchor: InteractionAnchor
    ): List<MessageOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun updateThreadName(name: String?, threadId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getAuthorizationStatus(): UserAuthorizationStatusDTO {
        TODO("Not yet implemented")
    }

    override suspend fun authorizeUsers(userIdentifiers: List<UserIdentifier>) {
        TODO("Not yet implemented")
    }

    override suspend fun blockUsers(userIdentifiers: List<UserIdentifier>) {
        TODO("Not yet implemented")
    }

}