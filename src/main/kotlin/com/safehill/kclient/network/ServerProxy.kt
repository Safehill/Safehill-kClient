package com.safehill.kclient.network

import com.safehill.kclient.GlobalIdentifier
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
import com.safehill.kclient.models.SHGenericEncryptedAsset
import com.safehill.kclient.models.SHLocalUser
import com.safehill.kclient.models.SHRemoteUser
import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.models.SHShareableEncryptedAsset
import com.safehill.kclient.models.SHUserReaction
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.network.dtos.RecipientEncryptionDetailsDTO
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
        groupId: String
    ): List<SHMessageOutputDTO> {
        return remoteServer.addMessages(messages, groupId).also {
            localServer.insertMessages(it, groupId)
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

    //
    /// Retrieve asset from local server (cache).
    ///
    /// /// If only a `.lowResolution` version is available, this method triggers the caching of the `.midResolution` in the background.
    /// In addition, when asking for a `.midResolution` or a `.hiResolution` version, and the `cacheHiResolution` parameter is set to `true`,
    /// this method triggers the caching in the background of the `.hiResolution` version, unless already availeble, replacing the `.midResolution`.
    /// Use the `cacheHiResolution` carefully, as higher resolution can take a lot of space on disk.
    ///
    /// - Parameters:
    ///   - assetIdentifiers: the global identifier of the asset to retrieve
    ///   - versions: the versions to retrieve
    ///   - cacheHiResolution: if the `.hiResolution` isn't in the local server, then fetch it and cache it in the background. `.hiResolution` is usually a big file, so this boolean lets clients control the caching strategy. Also, this parameter only makes sense when requesting `.midResolution` or `.hiResolution` versions. It's a no-op otherwise.
    ///   - completionHandler: the callback method returning the encrypted assets keyed by global id, or the error
    @OptIn(DelicateCoroutinesApi::class, DelicateCoroutinesApi::class)
    override suspend fun getLocalAssets(
        globalIdentifiers: List<GlobalIdentifier>,
        versions: List<SHAssetQuality>,
        cacheHiResolution: Boolean
    ): Map<String, SHEncryptedAsset> {
        val versionsToRetrieve = versions.toMutableSet()

        // If `.hiResolution` is explicitly requested and `.midResolution` is not requested, add `.midResolution` to versionsToRetrieve
        if (SHAssetQuality.HighResolution in versionsToRetrieve && SHAssetQuality.MidResolution !in versionsToRetrieve) {
            versionsToRetrieve.add(SHAssetQuality.MidResolution)
        }

        if (cacheHiResolution) {
            // If `.midResolution` is requested and `.hiResolution` is not requested, add `.hiResolution` to versionsToRetrieve
            if (SHAssetQuality.MidResolution in versionsToRetrieve && SHAssetQuality.HighResolution !in versionsToRetrieve) {
                versionsToRetrieve.add(SHAssetQuality.HighResolution)
            }
        }

        // Always add `.lowResolution`, even when not explicitly requested
        // to distinguish between assets without any version and assets with only `.lowResolution`
        // An asset with `.lowResolution` only will trigger the loading of the next quality version in the background
        versionsToRetrieve.add(SHAssetQuality.LowResolution)

        val map  = localServer.getAssets(globalIdentifiers, versions)

        // Always cache the `.midResolution` if the `.lowResolution` is the only version available
        map.forEach { (globalIdentifier, encryptedAsset) ->
            if (versionsToRetrieve.size > 1 &&
                encryptedAsset.encryptedVersions.size == 1 &&
                encryptedAsset.encryptedVersions.keys.first() == SHAssetQuality.LowResolution) {
                GlobalScope.launch(Dispatchers.Default) {
                    cacheAssets(listOf(globalIdentifier), SHAssetQuality.MidResolution)
                }
            }
        }

        // Cache the `.hiResolution` if requested
        if (cacheHiResolution) {
            val hiResGlobalIdentifiersToLazyLoad = map.filter { (_, encryptedAsset) ->
                versionsToRetrieve.contains(SHAssetQuality.HighResolution) &&
                        !encryptedAsset.encryptedVersions.containsKey(SHAssetQuality.HighResolution)
            }.keys.toList()

            if (hiResGlobalIdentifiersToLazyLoad.isNotEmpty()) {
                GlobalScope.launch(Dispatchers.Default) {
                    cacheAssets(hiResGlobalIdentifiersToLazyLoad, SHAssetQuality.HighResolution)
                }
            }
        }
        return organizeAssetVersions(map, versions)
    }

    private fun organizeAssetVersions(
        encryptedAssetsByGlobalId: Map<String, SHEncryptedAsset>,
        requestedVersions: List<SHAssetQuality>
    ): Map<String, SHEncryptedAsset> {
        val map = encryptedAssetsByGlobalId.mapValues { (_, encryptedAsset) ->
            val newEncryptedVersions = encryptedAsset.encryptedVersions.toMutableMap()

            if (SHAssetQuality.HighResolution in requestedVersions &&
                encryptedAsset.encryptedVersions.containsKey(SHAssetQuality.MidResolution) &&
                SHAssetQuality.HighResolution !in encryptedAsset.encryptedVersions) {
                // If `.hiResolution` was requested, use the `.midResolution` version if available under that key
                newEncryptedVersions[SHAssetQuality.HighResolution] = encryptedAsset.encryptedVersions[SHAssetQuality.MidResolution]!!

                // Populate the rest of the versions based on the `requestedVersions`
                requestedVersions.forEach { version ->
                    if (version != SHAssetQuality.HighResolution &&
                        encryptedAsset.encryptedVersions.containsKey(version)) {
                        newEncryptedVersions[version] = encryptedAsset.encryptedVersions[version]!!
                    }
                }
            }

            return@mapValues SHGenericEncryptedAsset(
                globalIdentifier = encryptedAsset.globalIdentifier,
                localIdentifier = encryptedAsset.localIdentifier,
                creationDate = encryptedAsset.creationDate,
                encryptedVersions = newEncryptedVersions
            )
        }
        return map

    }

    private fun cacheAssets(globalIdentifiers: List<String>, quality: SHAssetQuality) {
        //TODO("Not yet implemented")
    }

    override suspend fun getLocalAssetDescriptors(
        globalIdentifiers: List<GlobalIdentifier>?,
        filteringGroups: List<String>?
    ): List<SHAssetDescriptor> {
        return localServer.getAssetDescriptors(globalIdentifiers, filteringGroups)
    }
}
