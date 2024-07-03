package com.safehill.kclient.models.assets

import java.time.Instant

class DecryptedAssetImpl(
    override val globalIdentifier: AssetGlobalIdentifier,
    override val localIdentifier: AssetLocalIdentifier?,
    override val creationDate: Instant?,
    override var decryptedVersions: Map<AssetQuality, ByteArray>
) : DecryptedAsset
