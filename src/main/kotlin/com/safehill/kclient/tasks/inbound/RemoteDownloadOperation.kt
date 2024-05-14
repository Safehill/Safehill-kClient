package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.*
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.ServerProxy

public class RemoteDownloadOperation(
    val serverProxy: ServerProxy,
    override var listeners: List<DownloadOperationListener>
) : AbstractDownloadOperation() {

    companion object {
        var alreadyProcessed = mutableListOf<AssetGlobalIdentifier>()
    }

    override val user: LocalUser
        get() = serverProxy.requestor

    override suspend fun getDescriptors(): List<AssetDescriptor> {
        return serverProxy.remoteServer.getAssetDescriptors()
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
            .getAssetDescriptors()
            .map { it.globalIdentifier }
        val remoteOnlyDescriptors = descriptors
            .filter {
                !globalIdentifiersInLocalServer.contains(it.globalIdentifier)
                        && !alreadyProcessed.contains(it.globalIdentifier)
            }
        processAssetsInDescriptors(remoteOnlyDescriptors)
    }
}