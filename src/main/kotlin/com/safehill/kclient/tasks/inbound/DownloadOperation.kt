package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.models.users.ServerUser


public interface DownloadOperation {

    val listeners: List<DownloadOperationListener>

    val user: LocalUser

    suspend fun getDescriptors(): List<AssetDescriptor>
    suspend fun getUsers(
        withIdentifiers: List<UserIdentifier>
    ): Map<UserIdentifier, ServerUser>

    suspend fun getEncryptedAssets(
        withGlobalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>
    ): Map<AssetGlobalIdentifier, EncryptedAsset>

    suspend fun process(
        descriptors: List<AssetDescriptor>
    )

    suspend fun processAssetsInDescriptors(
        descriptors: List<AssetDescriptor>
    )
}