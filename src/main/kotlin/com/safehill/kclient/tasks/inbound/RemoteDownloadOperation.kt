package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetDescriptorsCache
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.network.ServerProxy

class RemoteDownloadOperation(
    assetDescriptorsCache: AssetDescriptorsCache,
    private val serverProxy: ServerProxy,
) : AbstractDownloadOperation(assetDescriptorsCache) {

    override val listeners: List<DownloadOperationListener> = mutableListOf()

    companion object {
        var alreadyProcessed = mutableListOf<AssetGlobalIdentifier>()
    }

    override suspend fun getDescriptors(): List<AssetDescriptor> {
        val remoteDescriptors =
            serverProxy.remoteServer.getAssetDescriptors(after = null)
        val globalIdentifiersInLocalServer = serverProxy.localServer
            .getAssetDescriptors(after = null)
            .map { it.globalIdentifier }
        val filteredDescriptors = remoteDescriptors
            .asSequence()
            .filter { it.globalIdentifier !in globalIdentifiersInLocalServer }
            .filter { it.globalIdentifier !in alreadyProcessed }
            .filter { it.uploadState.isDownloadable() }
            .toList()
        return filteredDescriptors
    }
}