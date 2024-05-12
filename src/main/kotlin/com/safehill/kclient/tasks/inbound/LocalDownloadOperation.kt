package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier

class LocalDownloadOperation(
    override var listeners: List<DownloadOperationListener>
) : DownloadOperation {
    override suspend fun fetchDescriptors(): List<AssetDescriptor> {
        TODO("Not yet implemented")
    }

    override suspend fun getUsers(withIdentifiers: List<UserIdentifier>): List<ServerUser> {
        TODO("Not yet implemented")
    }

    override suspend fun process(descriptors: List<AssetDescriptor>) {
        TODO("Not yet implemented")
    }

    override suspend fun processAssetsInDescriptors(descriptors: List<AssetDescriptor>) {
        TODO("Not yet implemented")
    }
}