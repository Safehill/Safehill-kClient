package com.safehill.kclient.network

import com.safehill.kclient.GlobalIdentifier
import com.safehill.kclient.api.SafehillApi
import com.safehill.kclient.models.SHAssetDescriptor
import com.safehill.kclient.models.SHAssetQuality
import com.safehill.kclient.models.SHEncryptedAsset
import com.safehill.kclient.models.SHServerUser

interface ServerProxyInterface : SafehillApi {
    suspend fun getAllLocalUsers(): List<SHServerUser>

    @Throws(Exception::class)
    suspend fun getLocalAssets(globalIdentifiers: List<GlobalIdentifier>, versions: List<SHAssetQuality>, cacheHiResolution: Boolean): Map<String, SHEncryptedAsset>
    @Throws(Exception::class)
    suspend fun getLocalAssetDescriptors(globalIdentifiers: List<GlobalIdentifier>? = null, filteringGroups: List<String>? = null): List<SHAssetDescriptor>
}

