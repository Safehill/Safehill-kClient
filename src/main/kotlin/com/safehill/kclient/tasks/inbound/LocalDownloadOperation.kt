package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.local.LocalServerInterface

public class LocalDownloadOperation(
    override val user: LocalUser,
    val localServer: LocalServerInterface,
    override var listeners: List<DownloadOperationListener>
) : AbstractDownloadOperation() {
    override suspend fun getDescriptors(): List<AssetDescriptor> {
        return localServer.getAssetDescriptors(after = null)
    }

    override suspend fun getUsers(withIdentifiers: List<UserIdentifier>): Map<UserIdentifier, ServerUser> {
        return localServer.getUsers(withIdentifiers)
    }

    override suspend fun getEncryptedAssets(
        withGlobalIdentifiers: List<AssetGlobalIdentifier>,
        versions: List<AssetQuality>,
    ): Map<AssetGlobalIdentifier, EncryptedAsset> {
        return localServer.getAssets(withGlobalIdentifiers, versions)
    }

    /**
     * For the full list of descriptors fetched from the local server
     * calls `processAssetsInDescriptors` for the remainder
     * @param descriptors the full list of descriptors fetched from the local server
     */
    override suspend fun process(descriptors: List<AssetDescriptor>) {
        processAssetsInDescriptors(descriptors)
    }

}