package com.safehill.kclient.models.assets

import java.util.Date

class EncryptedAssetImpl(
    override val globalIdentifier: String,
    override val localIdentifier: String?,
    override var creationDate: Date?,
    override var encryptedVersions: Map<AssetQuality, EncryptedAssetVersion>
) : EncryptedAsset

