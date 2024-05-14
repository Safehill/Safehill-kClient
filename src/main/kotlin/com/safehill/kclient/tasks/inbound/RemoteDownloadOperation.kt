package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.tasks.BackgroundTask

public class RemoteDownloadOperation(
    val serverProxy: ServerProxy,
    override var listeners: List<DownloadOperationListener>
) : DownloadOperation, BackgroundTask {

    override suspend fun fetchDescriptors(): List<AssetDescriptor> {
        return serverProxy.remoteServer.getAssetDescriptors()
    }

    override suspend fun getUsers(withIdentifiers: List<UserIdentifier>): List<ServerUser> {
        return serverProxy.getUsers(withIdentifiers)
    }

    override suspend fun process(descriptors: List<AssetDescriptor>) {
        processAssetsInDescriptors(descriptors)
    }

    override suspend fun processAssetsInDescriptors(descriptors: List<AssetDescriptor>) {
        TODO("Not yet implemented")
    }

    override suspend fun run() {
        val descriptors = fetchDescriptors()
        if (descriptors.isNotEmpty()) {
            process(descriptors)
        }
    }
}