package com.safehill.kclient.models.assets

import java.util.Date

interface EncryptedAsset: RemoteAssetIdentifiable {
    var creationDate: Date?
    var encryptedVersions: Map<AssetQuality, EncryptedAssetVersion>
}
