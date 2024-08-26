package com.safehill.kclient.tasks.inbound

import com.safehill.SafehillClient
import com.safehill.kclient.models.assets.AssetDescriptor

class LocalDownloadOperation(
    override var listeners: List<DownloadOperationListener>,
    override val safehillClient: SafehillClient,
) : AbstractDownloadOperation() {

    override suspend fun getDescriptors(): List<AssetDescriptor> {
        return safehillClient.serverProxy.localServer.getAssetDescriptors(after = null)
    }

}