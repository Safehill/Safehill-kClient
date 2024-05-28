package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.*
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.kclient.network.ServerProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
interface AssetActivityRestorationDelegate {
    // Notify the delegate that the restoration started.
    fun didStartRestoration()

    // Let the delegate know that a queue item representing a successful UPLOAD needs to be retrieved or re-created in the queue.
    // The item - in fact - might not exist in the queue if:
    // - the user logged out and the queues were cleaned
    // - the user is on another device
    fun restoreUploadQueueItems(forLocalIdentifiers: List<String>, groupId: String)

    // Let the delegate know that a queue item representing a successful SHARE needs to be retrieved or re-created in the queue.
    // The item - in fact - might not exist in the queue if:
    // - the user logged out and the queues were cleaned
    // - the user is on another device
    fun restoreShareQueueItems(forLocalIdentifiers: List<String>, groupId: String)

    // Notify the delegate that the restoration was completed.
    // This can be used as a signal to update all the threads, so the list of user identifiers
    // involved in the restoration of the upload/share requests is provided.
    fun didCompleteRestoration(userIdsInvolvedInRestoration: List<String>)
}

public class RemoteDownloadOperation(
    val serverProxy: ServerProxy,
    override var listeners: List<DownloadOperationListener>,
    val restorationDelegate: AssetActivityRestorationDelegate
) : AbstractDownloadOperation() {

    companion object {
        var alreadyProcessed = mutableListOf<AssetGlobalIdentifier>()
    }

    override val user: LocalUser
        get() = serverProxy.requestor

    override suspend fun getDescriptors(): List<AssetDescriptor> {
        return serverProxy.remoteServer.getAssetDescriptors(after = null)
    }

    override suspend fun getUsers(withIdentifiers: List<UserIdentifier>): Map<UserIdentifier, ServerUser> {
        // TODO: Define a user cache layer so that we don't have to make an HTTP call for cached users if this method is called multiple times
        return serverProxy.getUsers(withIdentifiers)
    }

    override suspend fun getEncryptedAssets(
        withGlobalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        return serverProxy.getAssets(withGlobalIdentifiers, versions)
    }

    /**
     * From the whole list of descriptors fetched from the server filters out descriptors
     * that have alrady been processed or that are present in the local server.
     * Calls `processAssetsInDescriptors` for the remainder
     * @param descriptors the full list of descriptors fetched from the server
     */
    override suspend fun process(descriptors: List<AssetDescriptor>) {
        val globalIdentifiersInLocalServer = serverProxy.localServer
            .getAssetDescriptors(after = null)
            .map { it.globalIdentifier }
        val remoteOnlyDescriptors = descriptors
            .filter {
                !globalIdentifiersInLocalServer.contains(it.globalIdentifier)
                        && !alreadyProcessed.contains(it.globalIdentifier)
            }
        processAssetsInDescriptors(remoteOnlyDescriptors)
    }


//
//    /// Given a list of descriptors determines which ones need to be dowloaded, authorized, or marked as backed up in the library.
//    /// Returns the list of descriptors for the assets that are ready to be downloaded
//    /// - Parameters:
//    ///   - descriptorsByGlobalIdentifier: the descriptors, keyed by asset global identifier
//    internal
//    suspend fun processAssetsInDescriptors(
//        descriptorsByGlobalIdentifier: Map<GlobalIdentifier, AssetDescriptor>,
//    ): Result<List<AssetDescriptor>> = withContext(Dispatchers.IO) {
//        if (descriptorsByGlobalIdentifier.isEmpty()) {
//            return@withContext Result.success(emptyList())
//        }
//
//        val sharedBySelfGlobalIdentifiers = mutableListOf<GlobalIdentifier>()
//        val sharedByOthersGlobalIdentifiers = mutableListOf<GlobalIdentifier>()
//
//        descriptorsByGlobalIdentifier.forEach { (globalIdentifier, descriptor) ->
//            if (descriptor.sharingInfo.sharedByUserIdentifier == user.identifier) {
//                sharedBySelfGlobalIdentifiers.add(globalIdentifier)
//            } else {
//                sharedByOthersGlobalIdentifiers.add(globalIdentifier)
//            }
//        }
//
//        val mergeResult = mergeDescriptorsWithAndroidPhotosAssets(
//            descriptorsByGlobalIdentifier = descriptorsByGlobalIdentifier,
//            filteringKeys = sharedBySelfGlobalIdentifiers
//        )
//
//        if (mergeResult.isFailure) {
//            return@withContext Result.failure(mergeResult.exceptionOrNull()!!)
//        }
//
//        val nonLocalPhotoLibraryGlobalIdentifiers = mergeResult.getOrNull() ?: emptyList()
//        val start = System.currentTimeMillis()
//
//        val processResult = processForDownload(
//            descriptorsByGlobalIdentifier = descriptorsByGlobalIdentifier,
//            nonAndroidPhotoLibrarySharedBySelfGlobalIdentifiers = nonLocalPhotoLibraryGlobalIdentifiers,
//            sharedBySelfGlobalIdentifiers = sharedBySelfGlobalIdentifiers,
//            sharedByOthersGlobalIdentifiers = sharedByOthersGlobalIdentifiers,
//        )
//
//        val end = System.currentTimeMillis()
//        println("[PERF] it took ${end - start} ms to process ${descriptorsByGlobalIdentifier.size} asset descriptors")
//
//        return@withContext processResult
//    }
//
//    suspend fun mergeDescriptorsWithAndroidPhotosAssets(
//        descriptorsByGlobalIdentifier: Map<GlobalIdentifier, AssetDescriptor>,
//        filteringKeys: List<GlobalIdentifier>
//    ): Result<List<GlobalIdentifier>> = withContext(Dispatchers.IO) {
//        if (descriptorsByGlobalIdentifier.isEmpty()) {
//            return@withContext Result.success(emptyList())
//        }
//
//        val filteredDescriptors = descriptorsByGlobalIdentifier.filter { filteringKeys.contains(it.key) }
//
//        if (filteredDescriptors.isEmpty()) {
//            return@withContext Result.success(emptyList())
//        }
//
//        val localIdentifiersInDescriptors = filteredDescriptors.values.mapNotNull { it.localIdentifier }
//        val androidAssetsByLocalIdentifier = mutableMapOf<String, AndroidAsset>()
//
//        try {
//            val fetchResult = fetchAllAssetsWithFilters(localIdentifiersInDescriptors)
//
//            fetchResult.forEach { androidAsset ->
//                androidAssetsByLocalIdentifier[androidAsset.localIdentifier] = androidAsset
//            }
//
//            val filteredGlobalIdentifiers = mutableListOf<GlobalIdentifier>()
//            val globalToAndroidAsset = mutableMapOf<GlobalIdentifier, AndroidAsset>()
//            filteredDescriptors.values.forEach { descriptor ->
//                val localId = descriptor.localIdentifier
//                val androidAsset = androidAssetsByLocalIdentifier[localId]
//                if (androidAsset != null) {
//                    globalToAndroidAsset[descriptor.globalIdentifier] = androidAsset
//                } else {
//                    filteredGlobalIdentifiers.add(descriptor.globalIdentifier)
//                }
//            }
//
//            notifyDownloaderDelegates(globalToAndroidAsset)
//
//            return@withContext Result.success(filteredGlobalIdentifiers)
//        } catch (e: Exception) {
//            return@withContext Result.failure(e)
//        }
//    }
//
//    suspend fun fetchAllAssetsWithFilters(localIdentifiers: List<String>): List<AndroidAsset> {
//        return suspendCoroutine { cont ->
//            // Simulate fetching assets, replace with actual implementation
//            val assets = listOf<AndroidAsset>() // Fetch assets based on the local identifiers
//            cont.resume(assets)
//        }
//    }
//
//    fun notifyDownloaderDelegates(globalToAndroidAssets: Map<GlobalIdentifier, AndroidAsset>) {
//        // Implementation of notifying delegates, similar to didIdentify
//        listeners.forEach {
//            it.didIdentify(globalToLocalAssets = globalToAndroidAssets)
//        }
//    }
//
//
//    suspend fun processForDownload(
//        descriptorsByGlobalIdentifier: Map<GlobalIdentifier, AssetDescriptor>,
//        nonAndroidPhotoLibrarySharedBySelfGlobalIdentifiers: List<GlobalIdentifier>,
//        sharedBySelfGlobalIdentifiers: List<GlobalIdentifier>,
//        sharedByOthersGlobalIdentifiers: List<GlobalIdentifier>,
//    ): Result<List<AssetDescriptor>> = withContext(Dispatchers.IO) {
//        val errors = mutableListOf<Throwable>()
//
//        // (1) Handle assets shared by this user and in the Android Photos library
//        val restorationResult = runCatching {
//            recreateLocalAssetsAndQueueItems(
//                descriptorsByGlobalIdentifier,
//                sharedBySelfGlobalIdentifiers,
//            )
//        }
//
//        if (restorationResult.isFailure) {
//            return@withContext Result.failure(restorationResult.exceptionOrNull()!!)
//        }
//
//        // (2) Collect items ready to download, starting from the ones shared by self
//        val descriptorsReadyToDownload = mutableListOf<AssetDescriptor>()
//        descriptorsReadyToDownload.addAll(
//            nonAndroidPhotoLibrarySharedBySelfGlobalIdentifiers.mapNotNull { descriptorsByGlobalIdentifier[it] }
//        )
//
//        // Handle assets shared by others
//        val authorizationResult = runCatching {
//            checkIfAuthorizationRequired(
//                sharedByOthersGlobalIdentifiers.mapNotNull { descriptorsByGlobalIdentifier[it] }
//            )
//        }
//
//        if (authorizationResult.isFailure) {
//            errors.add(authorizationResult.exceptionOrNull()!!)
//        } else {
//            descriptorsReadyToDownload.addAll(authorizationResult.getOrNull() ?: emptyList())
//        }
//
//        if (errors.isNotEmpty()) {
//            println("[${this::class.java.simpleName}] failed downloading assets with errors: ${errors.joinToString(",") { it.localizedMessage }}")
//            Result.failure(errors.first())
//        } else {
//            Result.success(descriptorsReadyToDownload)
//        }
//    }
//
//    suspend fun recreateLocalAssetsAndQueueItems(
//        descriptorsByGlobalIdentifier: Map<GlobalIdentifier, AssetDescriptor>,
//        globalIdentifiers: List<GlobalIdentifier>,
//    ): Result<Unit> = withContext(Dispatchers.IO) {
//        if (globalIdentifiers.isEmpty()) {
//            return@withContext Result.success(Unit)
//        }
//
//        val filteredDescriptors = descriptorsByGlobalIdentifier.filterKeys { globalIdentifiers.contains(it) }
//
//        if (filteredDescriptors.isEmpty()) {
//            return@withContext Result.success(Unit)
//        }
//
//        println("[${this::class.java.simpleName}] recreating local assets and queue items for $globalIdentifiers")
//
//        try {
//            val assetsDict = serverProxy.remoteServer.getAssets(globalIdentifiers, listOf(AssetQuality.LowResolution))
//            serverProxy.localServer.create(assetsDict.values.toList(), filteredDescriptors, AssetDescriptor.UploadState.Completed)
//            restoreAndRecreateQueueItems(filteredDescriptors)
//            Result.success(Unit)
//        } catch (e: Exception) {
//            println("[${this::class.java.simpleName}] failed to process assets: ${e.localizedMessage}")
//            Result.failure(e)
//        }
//    }
//
//
//    suspend fun restoreAndRecreateQueueItems(
//        descriptorsByGlobalIdentifier: Map<GlobalIdentifier, AssetDescriptor>,
//    ): Result<Unit> = withContext(Dispatchers.IO) {
//        val filteredDescriptors = descriptorsByGlobalIdentifier.filter {
//            it.value.sharingInfo.sharedByUserIdentifier == user.identifier
//        }
//
//        if (filteredDescriptors.isEmpty()) {
//            return@withContext Result.success(Unit)
//        }
//
//        val otherUsersById = mutableMapOf<String, ServerUser>()
//        val getUserResult = serverProxy.getUsers(filteredDescriptors.values.toList())
//
//        otherUsersById.putAll(getUserResult)
//
//        val remoteServerDescriptorByAssetGid = filteredDescriptors.filter {
//            it.value.localIdentifier != null
//        }
//
//        restorationDelegate.didStartRestoration()
//
//        val userIdsInvolvedInRestoration = mutableSetOf<String>()
//
//        for (remoteDescriptor in remoteServerDescriptorByAssetGid.values) {
//            val uploadLocalAssetIdByGroupId = mutableMapOf<String, MutableSet<String>>()
//            val shareLocalAssetIdsByGroupId = mutableMapOf<String, MutableSet<String>>()
//            val groupIdToUploadItem = mutableMapOf<String, Pair<UploadHistoryItem, Date>>()
//            val groupIdToShareItem = mutableMapOf<String, Pair<ShareHistoryItem, Date>>()
//
//            for ((recipientUserId, groupId) in remoteDescriptor.sharingInfo.sharedWithUserIdentifiersInGroup) {
//                val localIdentifier = remoteDescriptor.localIdentifier!!
//                val groupCreationDate = remoteDescriptor.sharingInfo.groupInfoById[groupId]!!.createdAt
//
//                if (recipientUserId == user.identifier) {
//                    uploadLocalAssetIdByGroupId.getOrPut(groupId) { mutableSetOf() }.add(localIdentifier)
//
//                    val item = QueueI(
//                        localAssetId = localIdentifier,
//                        globalAssetId = remoteDescriptor.globalIdentifier,
//                        versions = listOf(AssetQuality.LowResolution, AssetQuality.HighResolution),
//                        groupId = groupId,
//                        eventOriginator = user,
//                        sharedWith = emptyList(),
//                        isBackground = false
//                    )
//                    groupIdToUploadItem[groupId] = Pair(item, groupCreationDate)
//                } else {
//                    val user = otherUsersById[recipientUserId]
//                        ?: run {
//                            log.error("Inconsistency between user ids referenced in descriptors and user objects returned from server")
//                            continue
//                        }
//
//                    shareLocalAssetIdsByGroupId.getOrPut(groupId) { mutableSetOf() }.add(localIdentifier)
//                    groupIdToShareItem.merge(groupId, Pair(SHShareHistoryItem(
//                        localAssetId = localIdentifier,
//                        globalAssetId = remoteDescriptor.globalIdentifier,
//                        versions = listOf(AssetVersion.LOW_RESOLUTION, AssetVersion.HI_RESOLUTION),
//                        groupId = groupId,
//                        eventOriginator = myUser,
//                        sharedWith = listOf(user),
//                        isBackground = false
//                    ), groupCreationDate)) { old, new ->
//                        old.copy(first = old.first.copy(sharedWith = old.first.sharedWith + new.first.sharedWith))
//                    }
//
//                    userIdsInvolvedInRestoration.add(user.identifier)
//                }
//            }
//
//            for ((uploadItem, timestamp) in groupIdToUploadItem.values) {
//                try {
//                    uploadItem.insert(BackgroundOperationQueue.of(Type.SUCCESSFUL_UPLOAD), timestamp)
//                } catch (e: Exception) {
//                    log.warning("Unable to enqueue successful upload item groupId=${uploadItem.groupId}, localIdentifier=${uploadItem.localAssetId}")
//                }
//            }
//
//            for ((shareItem, timestamp) in groupIdToShareItem.values) {
//                try {
//                    shareItem.insert(BackgroundOperationQueue.of(Type.SUCCESSFUL_SHARE), timestamp)
//                } catch (e: Exception) {
//                    log.warning("Unable to enqueue successful share item groupId=${shareItem.groupId}, localIdentifier=${shareItem.localAssetId}")
//                }
//            }
//
//            log.debug("Upload local asset identifiers by group $uploadLocalAssetIdByGroupId")
//            log.debug("Share local asset identifiers by group $shareLocalAssetIdsByGroupId")
//
//            withContext(Dispatchers.Main) {
//                for ((groupId, localIdentifiers) in uploadLocalAssetIdByGroupId) {
//                    restorationDelegate.restoreUploadQueueItems(localIdentifiers.toList(), groupId)
//                }
//
//                for ((groupId, localIdentifiers) in shareLocalAssetIdsByGroupId) {
//                    restorationDelegate.restoreShareQueueItems(localIdentifiers.toList(), groupId)
//                }
//
//                restorationDelegate.didCompleteRestoration(userIdsInvolvedInRestoration.toList())
//            }
//        }
//
//        Result(Unit, null)
//    }
}