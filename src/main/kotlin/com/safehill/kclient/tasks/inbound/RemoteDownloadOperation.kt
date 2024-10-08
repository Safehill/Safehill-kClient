package com.safehill.kclient.tasks.inbound

import com.safehill.SafehillClient
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier

class RemoteDownloadOperation(
    override val safehillClient: SafehillClient,
    override val listeners: List<DownloadOperationListener>,
) : AbstractDownloadOperation() {

    companion object {
        var alreadyProcessed = mutableListOf<AssetGlobalIdentifier>()
    }

    override suspend fun getDescriptors(): List<AssetDescriptor> {
        val remoteDescriptors =
            safehillClient.serverProxy.remoteServer.getAssetDescriptors(after = null)
        val globalIdentifiersInLocalServer = safehillClient.serverProxy.localServer
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