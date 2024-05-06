package com.safehill.mock

import com.safehill.kclient.api.AssetGlobalIdentifier
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
import com.safehill.kclient.models.SHRemoteUser
import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.models.SHShareableEncryptedAsset
import com.safehill.kclient.models.SHUserReaction
import com.safehill.kclient.network.ServerProxyInterface
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.network.dtos.RecipientEncryptionDetailsDTO

// todo fix tests for implementation change

//class ServerProxySpy() : ServerProxyInterface {
//
//    var listTheadsCalled = 0
//    var listThreadResponse: List<ConversationThreadOutputDTO> = emptyList()
//    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
//        listTheadsCalled++
//        return listThreadResponse
//    }
//
//    var getUsersWithIdentifierCalled = 0
//    var getUsersWithIdentifierParam: List<String>? = null
//    var getUsersWithIdentifierResposne: List<SHRemoteUser> = emptyList()
//    override suspend fun getUsers(userIdentifiersToFetch: List<String>): List<SHRemoteUser> {
//        getUsersWithIdentifierCalled++
//        getUsersWithIdentifierParam = userIdentifiersToFetch
//        return getUsersWithIdentifierResposne
//    }
//
//    override suspend fun getUsersWithPhoneNumber(hashedPhoneNumbers: List<HashedPhoneNumber>): Map<HashedPhoneNumber, SHRemoteUser> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun searchUsers(query: String, per: Int, page: Int): List<SHRemoteUser> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getAssetDescriptors(): List<SHAssetDescriptor> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getAssetDescriptors(assetGlobalIdentifiers: List<AssetGlobalIdentifier>): List<SHAssetDescriptor> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getAssets(
//        globalIdentifiers: List<String>,
//        versions: List<SHAssetQuality>?
//    ): Map<String, SHEncryptedAsset> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun create(
//        assets: List<SHEncryptedAsset>,
//        groupId: String,
//        filterVersions: List<SHAssetQuality>?
//    ): List<SHAssetOutputDTO> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun share(asset: SHShareableEncryptedAsset) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun unshare(assetId: AssetGlobalIdentifier, userPublicIdentifier: String) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun retrieveThread(usersIdentifiers: List<String>): ConversationThreadOutputDTO? {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun retrieveThread(threadId: String): ConversationThreadOutputDTO? {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun createOrUpdateThread(
//        name: String?,
//        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
//    ): ConversationThreadOutputDTO {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun upload(
//        serverAsset: SHAssetOutputDTO,
//        asset: SHEncryptedAsset,
//        filterVersions: List<SHAssetQuality>
//    ) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun markAsset(
//        assetGlobalIdentifier: AssetGlobalIdentifier,
//        quality: SHAssetQuality,
//        asState: SHAssetDescriptorUploadState
//    ) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun deleteAssets(globalIdentifiers: List<String>): List<String> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun setGroupEncryptionDetails(
//        groupId: String,
//        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
//    ) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun deleteGroup(groupId: String) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun retrieveGroupUserEncryptionDetails(groupId: String): List<RecipientEncryptionDetailsDTO> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun addReactions(
//        reactions: List<SHUserReaction>,
//        toGroupId: String
//    ): List<SHReactionOutputDTO> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun removeReaction(reaction: SHUserReaction, fromGroupId: String) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun retrieveInteractions(
//        inGroupId: String,
//        per: Int,
//        page: Int,
//        before: String?
//    ): SHInteractionsGroupDTO {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun addMessages(
//        messages: List<SHMessageInputDTO>,
//        toGroupId: String
//    ): List<SHMessageOutputDTO> {
//        TODO("Not yet implemented")
//    }
//
//    var getAllLocalUsersCalled = 0
//    var getAllLocalUsersResponse: List<SHServerUser> = emptyList()
//    override suspend fun getAllLocalUsers(): List<SHServerUser> {
//        getAllLocalUsersCalled++
//        return getAllLocalUsersResponse
//    }
//
//    override var requestor: SHLocalUserInterface
//        get() = TODO("Not yet implemented")
//        set(value) {}
//
//    override suspend fun createUser(name: String): SHServerUser {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun sendCodeToUser(
//        countryCode: Int,
//        phoneNumber: Long,
//        code: String,
//        medium: SHSendCodeToUserRequestDTO.Medium
//    ) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun updateUser(
//        name: String?,
//        phoneNumber: String?,
//        email: String?
//    ): SHServerUser {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun deleteAccount(name: String, password: String) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun deleteAccount() {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun signIn(): SHAuthResponseDTO {
//        TODO("Not yet implemented")
//    }
//
//    fun reset() {
//        listTheadsCalled = 0
//        listThreadResponse = emptyList()
//        getUsersWithIdentifierCalled = 0
//        getUsersWithIdentifierParam = null
//        getUsersWithIdentifierResposne = emptyList()
//        getAllLocalUsersCalled = 0
//        getAllLocalUsersResponse = emptyList()
//    }
//}
