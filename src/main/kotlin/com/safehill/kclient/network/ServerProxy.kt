package com.safehill.kclient.network

import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.network.local.LocalServerInterface

typealias GlobalIdentifier = String

interface ServerProxy : SafehillApi {

    val localServer: LocalServerInterface
    val remoteServer: SafehillApi

    suspend fun getAsset(
        globalIdentifier: GlobalIdentifier,
        quality: AssetQuality,
        cacheAfterFetch: Boolean
    ): EncryptedAsset

    @Throws(Exception::class)
    suspend fun getLocalAssets(
        globalIdentifiers: List<GlobalIdentifier>,
        versions: List<AssetQuality>,
        cacheHiResolution: Boolean
    ): Map<String, EncryptedAsset>

}

