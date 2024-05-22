package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.ServerProxy

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