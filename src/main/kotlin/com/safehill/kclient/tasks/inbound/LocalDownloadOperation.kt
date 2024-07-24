package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.network.ServerProxy

class LocalDownloadOperation(
    private val serverProxy: ServerProxy,
    override var listeners: List<DownloadOperationListener>,
    userController: UserController
) : AbstractDownloadOperation(
    serverProxy = serverProxy,
    userController = userController
) {
    override val user = serverProxy.requestor

    override suspend fun getDescriptors(): List<AssetDescriptor> {
        return serverProxy.localServer.getAssetDescriptors(after = null)
    }

}