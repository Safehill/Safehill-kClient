package com.safehill.kclient.models.assets

import java.util.Date

class DecryptedAssetImpl(
    override val globalIdentifier: AssetGlobalIdentifier,
    override val localIdentifier: AssetLocalIdentifier?,
    override var creationDate: Date?,
    override var decryptedVersions: Map<AssetQuality, ByteArray>
) : DecryptedAsset

