package com.safehill.kclient.network

import com.safehill.kclient.controllers.UserInteractionController
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
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.users.RemoteUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.exceptions.SafehillError
import com.safehill.kclient.network.local.LocalServerInterface
import com.safehill.kclient.network.remote.RemoteServer
import com.safehill.kclient.util.Provider
import com.safehill.kclient.util.runCatchingSafe
import com.safehill.kclient.utils.setupBouncyCastle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant

class ServerProxyImpl(
    private val localServerProvider: Provider<LocalServerInterface>,
    override val remoteServer: RemoteServer
) : ServerProxy,
    // Delegates most of the functions to RemoteServer.
    // Override if different implementation is necessary.
    SafehillApi by remoteServer {

    init {
        setupBouncyCastle()
    }

    override val localServer: LocalServerInterface
        get() = localServerProvider.get()

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
        return runCatchingSafe {
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
                            anchorId = threadId,
                            messages = listOf(message)
                        )
                    }
                }
            }.awaitAll()
        }
    }

    override suspend fun retrieveThread(
        usersIdentifiers: List<UserIdentifier>,
        phoneNumbers: List<String>
    ): ConversationThreadOutputDTO? {
        return runCatchingSafe {
            remoteServer.retrieveThread(
                usersIdentifiers = usersIdentifiers,
                phoneNumbers = phoneNumbers
            )?.also {
                localServer.createOrUpdateThread(listOf(it))
            }
        }.getOrElse {
            localServer.retrieveThread(
                usersIdentifiers = usersIdentifiers,
                phoneNumbers = phoneNumbers
            )
        }
    }

    override suspend fun retrieveThread(threadId: String): ConversationThreadOutputDTO? {
        return localServer.retrieveThread(threadId) ?: remoteServer.retrieveThread(threadId)?.also {
            localServer.createOrUpdateThread(listOf(it))
        }
    }

    override suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>,
        phoneNumbers: List<String>
    ): ConversationThreadOutputDTO {
        return remoteServer.createOrUpdateThread(
            name = name,
            recipientsEncryptionDetails = recipientsEncryptionDetails,
            phoneNumbers = phoneNumbers
        ).also {
            localServer.createOrUpdateThread(listOf(it))
        }
    }

    /***
     * This function will try to fetch interactions from remote server and if it fails, retrieves locally.
     */
    override suspend fun retrieveInteractions(
        anchorId: String,
        interactionAnchor: InteractionAnchor,
        per: Int,
        page: Int,
        before: String?
    ): InteractionsGroupDTO {
        return try {
            remoteServer.retrieveInteractions(
                anchorId = anchorId,
                per = per,
                page = page,
                before = before,
                interactionAnchor = interactionAnchor
            ).also {
                localServer.insertMessages(
                    messages = it.messages,
                    anchorId = anchorId
                )
            }
        } catch (e: Exception) {
            println("failed to fetch interactions from server. Returning local version. $e ${e.message}")
            localServer.retrieveInteractions(
                interactionAnchor = interactionAnchor,
                anchorId = anchorId,
                per = per,
                page = page,
                before = before
            )
        }
    }

    private fun Map<AssetGlobalIdentifier, EncryptedAsset>.getMissingAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>
    ): Map<GlobalIdentifier, List<AssetQuality>> {
        return buildMap {
            val assetIdentifiersNotFoundInLocal = (globalIdentifiers - this.keys)
            putAll(assetIdentifiersNotFoundInLocal.associateWith { versions })
            this@getMissingAssets.forEach { (globalIdentifier, encryptedAsset) ->
                val versionsNotFoundInLocal = (versions - encryptedAsset.encryptedVersions.keys)
                if (versionsNotFoundInLocal.isNotEmpty()) {
                    put(globalIdentifier, versionsNotFoundInLocal)
                }
            }
        }
    }

    private suspend fun fetchRemoteAssets(
        globalIdentifiersAndQualities: Map<AssetGlobalIdentifier, List<AssetQuality>>,
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        return coroutineScope {
            globalIdentifiersAndQualities.map { (globalIdentifier, assetQualities) ->
                async {
                    runCatchingSafe {
                        remoteServer.getEncryptedAssets(
                            globalIdentifiers = listOf(globalIdentifier),
                            versions = assetQualities
                        )[globalIdentifier]
                    }.onFailure {
                        println("Failed to fetch asset with id $globalIdentifier and quality $assetQualities")
                    }.getOrNull()
                }
            }.awaitAll()
                .filterNotNull()
                .associateBy { it.globalIdentifier }
        }
    }

    override suspend fun updateThreadName(
        name: String?,
        threadId: String
    ) {
        remoteServer.updateThreadName(name = name, threadId = threadId)
            .also { localServer.updateThreadName(name = name, threadId = threadId) }
    }

    private suspend fun getAssetsFromRemoteAndStore(
        globalIdentifiersAndQualities: Map<AssetGlobalIdentifier, List<AssetQuality>>,
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        val remoteDescriptors = remoteServer.getAssetDescriptors(
            assetGlobalIdentifiers = globalIdentifiersAndQualities.keys.toList(),
            groupIds = null, after = null
        )

        val identifiersToFetch = remoteDescriptors.map { it.globalIdentifier }
        val assetIdentifiersToFetch =
            globalIdentifiersAndQualities.filter { it.key in identifiersToFetch }

        val remoteAssets = fetchRemoteAssets(assetIdentifiersToFetch)
        localServer.storeAssetsWithDescriptor(
            encryptedAssetsWithDescriptor = remoteDescriptors.mapNotNull { assetDescriptor ->
                remoteAssets[assetDescriptor.globalIdentifier]?.let { encryptedAsset ->
                    assetDescriptor to encryptedAsset
                }
            }.toMap()
        )
        return remoteAssets
    }

    override suspend fun getEncryptedAssets(
        globalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        val localAssets = localServer.getEncryptedAssets(
            globalIdentifiers = globalIdentifiers,
            versions = versions
        )
        val assetsNotFoundInLocal = localAssets.getMissingAssets(
            globalIdentifiers = globalIdentifiers, versions = versions
        ).also { if (it.isEmpty()) return localAssets }

        val remoteAssets = getAssetsFromRemoteAndStore(
            globalIdentifiersAndQualities = assetsNotFoundInLocal
        )

        return combineLocalAndRemoteAssets(
            remoteAssets = remoteAssets,
            localAssets = localAssets
        )
    }

    private fun combineLocalAndRemoteAssets(
        remoteAssets: Map<AssetGlobalIdentifier, EncryptedAsset>,
        localAssets: Map<AssetGlobalIdentifier, EncryptedAsset>
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        return buildMap {
            putAll(remoteAssets)
            localAssets.forEach { (globalIdentifier, encryptedLocalAsset) ->
                val existingEncryptedAsset = remoteAssets[globalIdentifier]
                if (existingEncryptedAsset != null) {
                    this[globalIdentifier] = existingEncryptedAsset.copy(
                        encryptedVersions = existingEncryptedAsset.encryptedVersions + encryptedLocalAsset.encryptedVersions
                    )
                } else {
                    this[globalIdentifier] = encryptedLocalAsset
                }
            }
        }
    }

    override suspend fun addMessages(
        messages: List<MessageInputDTO>,
        anchorId: String,
        interactionAnchor: InteractionAnchor
    ): List<MessageOutputDTO> {
        return remoteServer.addMessages(messages, anchorId, interactionAnchor).also {
            localServer.insertMessages(it, anchorId)
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
        qualities: List<AssetQuality>,
        cacheAfterFetch: Boolean
    ): EncryptedAsset {
        val localEncryptedAsset = localServer.getEncryptedAssets(
            globalIdentifiers = listOf(globalIdentifier),
            versions = qualities
        )[globalIdentifier]

        val localVersions = localEncryptedAsset?.encryptedVersions ?: mapOf()

        val remainingQualities = qualities - localVersions.keys
        return if (localEncryptedAsset != null && remainingQualities.isEmpty()) {
            localEncryptedAsset
        } else {
            val remoteAsset = remoteServer.getEncryptedAssets(
                globalIdentifiers = listOf(globalIdentifier),
                versions = remainingQualities
            )[globalIdentifier]
            if (remoteAsset == null) {
                localEncryptedAsset ?: throw SafehillError.ClientError.NotFound
            } else {
                if (cacheAfterFetch) {
                    val remoteDescriptor = getAssetDescriptors(
                        assetGlobalIdentifiers = listOf(globalIdentifier),
                        groupIds = null, after = null
                    ).first()
                    localServer.storeAssetsWithDescriptor(
                        encryptedAssetsWithDescriptor = mapOf(remoteDescriptor to remoteAsset)
                    )
                }
                remoteAsset.copy(
                    encryptedVersions = remoteAsset.encryptedVersions + localVersions
                )
            }
        }
    }


    override suspend fun retrieveGroupUserEncryptionDetails(groupId: GroupId): RecipientEncryptionDetailsDTO {
        return try {
            localServer.retrieveGroupUserEncryptionDetails(groupId = groupId)
        } catch (_: UserInteractionController.InteractionErrors.MissingE2EEDetails) {
            remoteServer.retrieveGroupUserEncryptionDetails(groupId = groupId).also {
                localServer.setGroupEncryptionDetails(
                    groupId = groupId,
                    recipientsEncryptionDetails = listOf(it)
                )
            }
        }
    }

    override suspend fun deleteThread(threadId: String) {
        remoteServer.deleteThread(threadId = threadId).also {
            localServer.deleteThread(threadId = threadId)
        }
    }
}
