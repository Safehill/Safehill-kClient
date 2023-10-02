package com.safehill.kclient.models

import java.util.Date

class SHEncryptedAssetImpl(
    override val globalIdentifier: String,
    override val localIdentifier: String?,
    override var creationDate: Date?,
    override var encryptedVersions: Map<SHAssetQuality, SHEncryptedAssetVersion>
) : SHEncryptedAsset

