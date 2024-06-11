package com.safehill.kclient.models.assets

import java.time.Instant

interface DecryptedAsset: RemoteAssetIdentifiable {
    var decryptedVersions: Map<AssetQuality, ByteArray>
    val creationDate: Instant?
}
