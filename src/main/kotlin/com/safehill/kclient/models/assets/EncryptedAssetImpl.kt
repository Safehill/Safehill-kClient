package com.safehill.kclient.models.assets

import java.time.Instant

class EncryptedAssetImpl(
    override val globalIdentifier: AssetGlobalIdentifier,
    override val localIdentifier: AssetLocalIdentifier?,
    override var creationDate: Instant?,
    override var encryptedVersions: Map<AssetQuality, EncryptedAssetVersion>
) : EncryptedAsset
