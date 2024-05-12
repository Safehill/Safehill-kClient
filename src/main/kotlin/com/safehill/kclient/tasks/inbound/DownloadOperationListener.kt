package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.*
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.models.users.ServerUser


public interface DownloadOperationListener {
    fun received(
        assetDescriptors: List<AssetDescriptor>,
        referencingUsers: Map<UserIdentifier, ServerUser>
    )

    fun didIdentify(
        globalToLocalAssets: Map<AssetLocalIdentifier, AssetGlobalIdentifier>
    )

    fun didFetch(
        decryptedAsset: DecryptedAsset
    )
}