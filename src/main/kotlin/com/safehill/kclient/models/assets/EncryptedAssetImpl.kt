package com.safehill.kclient.models.assets

import java.util.Date

class EncryptedAssetImpl(
    override val globalIdentifier: AssetGlobalIdentifier,
    override val localIdentifier: AssetLocalIdentifier?,
    override var creationDate: Date?,
    override var encryptedVersions: Map<AssetQuality, EncryptedAssetVersion>
) : EncryptedAsset
