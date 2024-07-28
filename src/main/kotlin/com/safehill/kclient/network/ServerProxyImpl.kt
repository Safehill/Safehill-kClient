package com.safehill.kclient.network

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.ConversationThreadAssetsDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.InteractionsGroupDTO
import com.safehill.kclient.models.dtos.InteractionsSummaryDTO
import com.safehill.kclient.models.dtos.MessageInputDTO
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.exceptions.SafehillError
import com.safehill.kclient.network.local.LocalServerInterface
import com.safehill.kclient.util.runCatchingPreservingCancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

class ServerProxyImpl(
    override val localServer: LocalServerInterface,
    override val remoteServer: SafehillApi,
    override val requestor: LocalUser,
) : ServerProxy,
    // Delegates most of the functions to RemoteServer.
    // Override if different implementation is necessary.
    SafehillApi by remoteServer {


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

    override suspend fun getAssets(threadId: String): ConversationThreadAssetsDTO {
        return try {
            remoteServer.getAssets(threadId).also {
                localServer.storeThreadAssets(
                    threadId = threadId,
                    conversationThreadAssetsDTO = it
                )
            }
        } catch (e: Exception) {
            println("failed to fetch assets from server. Returning local version. $e ${e.message}")
            localServer.getAssets(threadId = threadId)
        }
    }


    override suspend fun topLevelInteractionsSummary(): InteractionsSummaryDTO {
        return runCatchingPreservingCancellationException {
            remoteServer.topLevelInteractionsSummary().also {
                storeInteractionSummary(it)
            }
        }.getOrElse {
            localServer.topLevelInteractionsSummary()
        }
    }

    private suspend fun storeInteractionSummary(interactionsSummaryDTO: InteractionsSummaryDTO) {
        coroutineScope {
            interactionsSummaryDTO.summaryByThreadId.map { (threadId, threadInteractionSummary) ->
                async {
                    localServer.createOrUpdateThread(listOf(threadInteractionSummary.thread))
                    val message = threadInteractionSummary.lastEncryptedMessage
                    if (message != null) {
                        localServer.insertMessages(
                            threadId = threadId,
                            messages = listOf(message)
                        )
                    }
                }
            }.awaitAll()
        }
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
            remoteServer.retrieveInteractions(
                inGroupId = inGroupId,
                per = per,
                page = page,
                before = before
            ).also {
                localServer.insertMessages(
                    messages = it.messages,
                    threadId = inGroupId
                )
            }
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

    override suspend fun getAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>?
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        val localAssets = localServer.getAssets(
            globalIdentifiers = globalIdentifiers,
            versions = versions
        )
        val assetIdentifiersNotFoundInLocal = globalIdentifiers - localAssets.keys
        if (assetIdentifiersNotFoundInLocal.isEmpty()) {
            return localAssets
        }
        val remoteDescriptors = remoteServer.getAssetDescriptors(
            assetGlobalIdentifiers = assetIdentifiersNotFoundInLocal,
            groupIds = null, after = null
        )
        val remoteAssets = remoteServer.getAssets(
            globalIdentifiers = assetIdentifiersNotFoundInLocal,
            versions = versions
        ).also { remoteAssets ->
            localServer.storeAssetsWithDescriptor(
                encryptedAssetsWithDescriptor = remoteDescriptors.mapNotNull { assetDescriptor ->
                    remoteAssets[assetDescriptor.globalIdentifier]?.let { encryptedAsset ->
                        assetDescriptor to encryptedAsset
                    }
                }.toMap()
            )
        }
        return localAssets + remoteAssets
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

    override suspend fun getAssetDescriptors(
        assetGlobalIdentifiers: List<AssetGlobalIdentifier>?,
        groupIds: List<GroupId>?,
        after: Instant?
    ): List<AssetDescriptor> {
        // All asset descriptors are being fetched.
        // Directly retrieve all descriptors from remote server.
        return if (assetGlobalIdentifiers == null) {
            remoteServer.getAssetDescriptors(
                assetGlobalIdentifiers = null,
                groupIds = groupIds,
                after = after
            )
        } else {
            val locallyAvailableDescriptors = localServer.getAssetDescriptors(
                assetGlobalIdentifiers = assetGlobalIdentifiers,
                groupIds = groupIds,
                after = after
            )
            val remainingDescriptors =
                assetGlobalIdentifiers - locallyAvailableDescriptors.map { it.globalIdentifier }
                    .toSet()
            if (remainingDescriptors.isEmpty()) {
                locallyAvailableDescriptors
            } else {
                remoteServer.getAssetDescriptors(
                    assetGlobalIdentifiers = remainingDescriptors,
                    groupIds = groupIds,
                    after = after
                )
            }
        }
    }

    override suspend fun getAsset(
        globalIdentifier: GlobalIdentifier,
        quality: AssetQuality,
        cacheAfterFetch: Boolean
    ): EncryptedAsset {
        return localServer.getAssets(
            globalIdentifiers = listOf(globalIdentifier),
            versions = listOf(quality)
        )[globalIdentifier] ?: run {
            val remoteAsset = remoteServer.getAssets(
                globalIdentifiers = listOf(globalIdentifier),
                versions = listOf(quality)
            )[globalIdentifier] ?: throw SafehillError.ClientError.NotFound
            if (cacheAfterFetch) {
                val remoteDescriptor = getAssetDescriptors(
                    assetGlobalIdentifiers = listOf(globalIdentifier),
                    groupIds = null, after = null
                ).first()
                localServer.storeAssetsWithDescriptor(
                    encryptedAssetsWithDescriptor = mapOf(remoteDescriptor to remoteAsset)
                )
            }
            remoteAsset
        }
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
        versions: List<AssetQuality>,
        cacheHiResolution: Boolean
    ): Map<String, EncryptedAsset> {
        val versionsToRetrieve = versions.toMutableSet()

        // If `.hiResolution` is explicitly requested and `.midResolution` is not requested, add `.midResolution` to versionsToRetrieve
        if (AssetQuality.HighResolution in versionsToRetrieve && AssetQuality.MidResolution !in versionsToRetrieve) {
            versionsToRetrieve.add(AssetQuality.MidResolution)
        }

        if (cacheHiResolution) {
            // If `.midResolution` is requested and `.hiResolution` is not requested, add `.hiResolution` to versionsToRetrieve
            if (AssetQuality.MidResolution in versionsToRetrieve && AssetQuality.HighResolution !in versionsToRetrieve) {
                versionsToRetrieve.add(AssetQuality.HighResolution)
            }
        }

        // Always add `.lowResolution`, even when not explicitly requested
        // to distinguish between assets without any version and assets with only `.lowResolution`
        // An asset with `.lowResolution` only will trigger the loading of the next quality version in the background
        versionsToRetrieve.add(AssetQuality.LowResolution)

        val map = localServer.getAssets(globalIdentifiers, versions)

        // Always cache the `.midResolution` if the `.lowResolution` is the only version available
        map.forEach { (globalIdentifier, encryptedAsset) ->
            if (versionsToRetrieve.size > 1 &&
                encryptedAsset.encryptedVersions.size == 1 &&
                encryptedAsset.encryptedVersions.keys.first() == AssetQuality.LowResolution
            ) {
                GlobalScope.launch(Dispatchers.Default) {
                    cacheAssets(listOf(globalIdentifier), AssetQuality.MidResolution)
                }
            }
        }

        // Cache the `.hiResolution` if requested
        if (cacheHiResolution) {
            val hiResGlobalIdentifiersToLazyLoad = map.filter { (_, encryptedAsset) ->
                versionsToRetrieve.contains(AssetQuality.HighResolution) &&
                        !encryptedAsset.encryptedVersions.containsKey(AssetQuality.HighResolution)
            }.keys.toList()

            if (hiResGlobalIdentifiersToLazyLoad.isNotEmpty()) {
                GlobalScope.launch(Dispatchers.Default) {
                    cacheAssets(hiResGlobalIdentifiersToLazyLoad, AssetQuality.HighResolution)
                }
            }
        }
        return organizeAssetVersions(map, versions)
    }

    private fun organizeAssetVersions(
        encryptedAssetsByGlobalId: Map<String, EncryptedAsset>,
        requestedVersions: List<AssetQuality>
    ): Map<String, EncryptedAsset> {
        val map = encryptedAssetsByGlobalId.mapValues { (_, encryptedAsset) ->
            val newEncryptedVersions = encryptedAsset.encryptedVersions.toMutableMap()

            if (AssetQuality.HighResolution in requestedVersions &&
                encryptedAsset.encryptedVersions.containsKey(AssetQuality.MidResolution) &&
                AssetQuality.HighResolution !in encryptedAsset.encryptedVersions
            ) {
                // If `.hiResolution` was requested, use the `.midResolution` version if available under that key
                newEncryptedVersions[AssetQuality.HighResolution] =
                    encryptedAsset.encryptedVersions[AssetQuality.MidResolution]!!

                // Populate the rest of the versions based on the `requestedVersions`
                requestedVersions.forEach { version ->
                    if (version != AssetQuality.HighResolution &&
                        encryptedAsset.encryptedVersions.containsKey(version)
                    ) {
                        newEncryptedVersions[version] = encryptedAsset.encryptedVersions[version]!!
                    }
                }
            }

            return@mapValues EncryptedAsset(
                globalIdentifier = encryptedAsset.globalIdentifier,
                localIdentifier = encryptedAsset.localIdentifier,
                creationDate = encryptedAsset.creationDate,
                encryptedVersions = newEncryptedVersions
            )
        }
        return map

    }

    private fun cacheAssets(globalIdentifiers: List<String>, quality: AssetQuality) {
        //TODO("Not yet implemented")
    }

}
