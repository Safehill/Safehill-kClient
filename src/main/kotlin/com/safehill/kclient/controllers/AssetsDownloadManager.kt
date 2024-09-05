package com.safehill.kclient.controllers

import com.safehill.kclient.errors.DownloadError
import com.safehill.kclient.models.DownloadBlacklist
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.DecryptedAsset

class AssetsDownloadManager(
    private val localAssetsStoreController: LocalAssetsStoreController
) {

    private val downloadBlackList = DownloadBlacklist()

    suspend fun downloadAsset(
        descriptor: AssetDescriptor
    ): Result<DecryptedAsset> {
        val isBlackListed = downloadBlackList.isBlacklisted(
            identifier = descriptor.globalIdentifier
        )
        return if (isBlackListed) {
            Result.failure(DownloadError.IsBlacklisted(identifier = descriptor.globalIdentifier))
        } else {
            localAssetsStoreController.getAsset(
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