package com.safehill.kclient.models.assets

import java.util.Date

interface DecryptedAsset: RemoteAssetIdentifiable {
    val decryptedVersions: Map<AssetQuality, ByteArray>
    val creationDate: Date?
}
