package com.safehill.kclient.models.assets

import java.time.Instant

data class EncryptedAsset(
    val globalIdentifier: AssetGlobalIdentifier,
    val localIdentifier: AssetLocalIdentifier?,
    val creationDate: Instant?,
    val encryptedVersions: Map<AssetQuality, EncryptedAssetVersion>
)
