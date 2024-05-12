package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.models.users.ServerUser


interface DownloadOperation {

    var listeners: List<DownloadOperationListener>

    suspend fun fetchDescriptors(): List<AssetDescriptor>
    suspend fun getUsers(
        withIdentifiers: List<UserIdentifier>
    ): List<ServerUser>

    suspend fun process(
        descriptors: List<AssetDescriptor>
    )

    suspend fun processAssetsInDescriptors(
        descriptors: List<AssetDescriptor>
    )
}