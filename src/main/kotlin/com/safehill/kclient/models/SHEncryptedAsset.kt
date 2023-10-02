package com.safehill.kclient.models

import java.util.Date

interface SHEncryptedAsset: SHRemoteAssetIdentifiable {
    var creationDate: Date?
    var encryptedVersions: Map<SHAssetQuality, SHEncryptedAssetVersion>
}
