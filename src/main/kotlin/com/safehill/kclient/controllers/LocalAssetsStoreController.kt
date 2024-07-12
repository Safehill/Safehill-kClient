package com.safehill.kclient.controllers

import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.kclient.network.ServerProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class LocalAssetsStoreController(
    private var serverProxy: ServerProxy,
    private var user: LocalUser,
) {
    suspend fun encryptedAsset(
        globalIdentifier: GlobalIdentifier,
        versions: List<AssetQuality>? = null,
        cacheHiResolution: Boolean,
    ): EncryptedAsset? {
        val encryptedAssets = encryptedAssets(
            globalIdentifiers = listOf(globalIdentifier),
            versions = versions,
            cacheHiResolution = cacheHiResolution,
        )
        return encryptedAssets[globalIdentifier]
    }

    suspend fun encryptedAssets(
        globalIdentifiers: List<GlobalIdentifier>,
        versions: List<AssetQuality>? = null,
        cacheHiResolution: Boolean,
    ): Map<GlobalIdentifier, EncryptedAsset> {
        return CoroutineScope(Dispatchers.IO).async {
            return@async serverProxy.getLocalAssets(
                globalIdentifiers = globalIdentifiers,
                versions = versions ?: AssetQuality.entries,
                cacheHiResolution = cacheHiResolution
            )
        }.await()
    }


}
