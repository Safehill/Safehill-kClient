package com.safehill.kclient.controllers

import com.safehill.SafehillClient
import com.safehill.kclient.errors.DownloadError
import com.safehill.kclient.models.DownloadBlacklist
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.DecryptedAsset

class AssetsDownloadManager(
    safehillClient: SafehillClient
) {

    private val downloadBlackList = DownloadBlacklist()

    private val localAssetStoreController = LocalAssetsStoreController(
        safehillClient = safehillClient
    )

    suspend fun downloadAsset(
        descriptor: AssetDescriptor
    ): Result<DecryptedAsset> {
        val isBlackListed = downloadBlackList.isBlacklisted(
            identifier = descriptor.globalIdentifier
        )
        return if (isBlackListed) {
            Result.failure(DownloadError.IsBlacklisted(identifier = descriptor.globalIdentifier))
        } else {
            localAssetStoreController.getAsset(
                globalIdentifier = descriptor.globalIdentifier,
                quality = AssetQuality.LowResolution,
                descriptor = descriptor,
                cacheAfterFetch = true
            ).onSuccess {
                downloadBlackList.removeFromBackList(identifier = descriptor.globalIdentifier)
            }.onFailure {
                downloadBlackList.recordFailedAttempt(
                    identifier = descriptor.globalIdentifier
                )
            }
        }
    }


}