package com.safehill.kclient.models.assets

import java.time.Instant

interface EncryptedAsset : RemoteAssetIdentifiable {
    var creationDate: Instant?
    var encryptedVersions: Map<AssetQuality, EncryptedAssetVersion>
}
