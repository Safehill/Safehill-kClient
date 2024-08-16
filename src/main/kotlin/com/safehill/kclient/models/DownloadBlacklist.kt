package com.safehill.kclient.models

import com.safehill.kclient.models.assets.AssetGlobalIdentifier

private typealias FailedCount = Int

class DownloadBlacklist {


    private val repeatedDownloadFailuresByAssetID =
        mutableMapOf<AssetGlobalIdentifier, FailedCount>()

    fun recordFailedAttempt(identifier: AssetGlobalIdentifier) {
        repeatedDownloadFailuresByAssetID[identifier] = getFailedCount(identifier) + 1
    }

    fun blackList(identifier: AssetGlobalIdentifier) {
        repeatedDownloadFailuresByAssetID[identifier] = FAILED_DOWNLOAD_COUNT_THRESHOLD
    }

    private fun getFailedCount(identifier: AssetGlobalIdentifier): FailedCount {
        return repeatedDownloadFailuresByAssetID.getOrDefault(
            key = identifier,
            defaultValue = 0
        )
    }

    fun removeFromBackList(identifier: AssetGlobalIdentifier) {
        repeatedDownloadFailuresByAssetID.remove(key = identifier)
    }

    fun areBlackListed(identifiers: List<AssetGlobalIdentifier>): Map<AssetGlobalIdentifier, Boolean> {
        return identifiers.associateWith { identifier -> isBlacklisted(identifier) }
    }

    fun isBlacklisted(identifier: AssetGlobalIdentifier): Boolean {
        return getFailedCount(identifier) >= FAILED_DOWNLOAD_COUNT_THRESHOLD
    }

    companion object {
        const val FAILED_DOWNLOAD_COUNT_THRESHOLD = 5
    }
}