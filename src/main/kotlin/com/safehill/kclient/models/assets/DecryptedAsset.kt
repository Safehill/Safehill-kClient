package com.safehill.kclient.models.assets

import java.util.Date

interface DecryptedAsset: RemoteAssetIdentifiable {
    var decryptedVersions: Map<AssetQuality, ByteArray>
    val creationDate: Date?
}
