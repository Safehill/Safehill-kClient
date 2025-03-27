package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetDescriptorsCache
import com.safehill.kclient.network.ServerProxy

class LocalDownloadOperation(
    private val serverProxy: ServerProxy,
    assetDescriptorsCache: AssetDescriptorsCache
) : AbstractDownloadOperation(assetDescriptorsCache) {

    override val listeners: List<DownloadOperationListener> = listOf()

    override suspend fun getDescriptors(): List<AssetDescriptor> {
        return serverProxy.localServer.getAssetDescriptors(after = null)
    }

}