package com.safehill.kclient.models.assets

import java.time.Instant

data class DecryptedAsset(
    val globalIdentifier: AssetGlobalIdentifier,
    val localIdentifier: AssetLocalIdentifier?,
    val creationDate: Instant?,
    var decryptedVersions: Map<AssetQuality, ByteArray>
)
