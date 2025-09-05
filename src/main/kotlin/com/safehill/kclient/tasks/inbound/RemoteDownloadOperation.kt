package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetDescriptorsCache
import com.safehill.kclient.network.ServerProxy

class RemoteDownloadOperation(
    assetDescriptorsCache: AssetDescriptorsCache,
    private val serverProxy: ServerProxy,
) : AbstractDownloadOperation(assetDescriptorsCache) {

    override val listeners: List<DownloadOperationListener> = mutableListOf()

    override suspend fun getDescriptors(): List<AssetDescriptor> {
        val remoteDescriptors =
            serverProxy.remoteServer.getAssetDescriptors(after = null)
        remoteDescriptors.forEach { descriptor ->
            serverProxy.localServer.storeAssetDescriptor(descriptor)
        }
        val filteredDescriptors = remoteDescriptors
            .filter { it.uploadState.isDownloadable() }
        return filteredDescriptors
    }
}