package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.DecryptedAsset
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier


public interface DownloadOperationListener {
    fun received(
        assetDescriptors: List<AssetDescriptor>,
        referencingUsers: Map<UserIdentifier, ServerUser>
    )

    fun fetched(
        decryptedAsset: DecryptedAsset
    )

}