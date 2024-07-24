package com.safehill.kclient.errors

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetGlobalIdentifier

sealed class DownloadError : Exception() {
    data class IsBlacklisted(
        val identifier: AssetGlobalIdentifier
    ) : DownloadError()

    data class SharedByUserNotFound(
        val assetDescriptor: AssetDescriptor
    ) : DownloadError()

    data class AssetNotFound(
        val assetDescriptor: AssetDescriptor
    ) : DownloadError()
}