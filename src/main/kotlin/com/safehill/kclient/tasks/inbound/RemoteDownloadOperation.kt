package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
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

class RemoteDownloadOperation(
    val serverProxy: ServerProxy,
    override val listeners: List<DownloadOperationListener>,
    private val userController: UserController
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
        return userController.getUsers(withIdentifiers).getOrThrow()
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
}