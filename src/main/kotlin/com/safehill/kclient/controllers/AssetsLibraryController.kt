package com.safehill.kclient.controllers

import com.safehill.kclient.models.assets.AssetDescriptorsCache
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.util.runCatchingSafe
import com.safehill.safehillclient.SafehillClient
import com.safehill.safehillclient.utils.extensions.assetModule
import com.safehill.safehillclient.utils.extensions.serverProxy

class AssetsLibraryController(
    private val assetDescriptorsCache: AssetDescriptorsCache,
    private val serverProxy: ServerProxy
) {

    suspend fun deleteAssets(globalIdentifiers: List<AssetGlobalIdentifier>): Result<Unit> {
        return runCatchingSafe {
            serverProxy.deleteAssets(globalIdentifiers)
            assetDescriptorsCache.removeDescriptor(globalIdentifiers)
        }
    }
}

val SafehillClient.assetsLibraryController
    get() = AssetsLibraryController(
        assetDescriptorsCache = assetModule.assetDescriptorCache,
        serverProxy = serverProxy
    )