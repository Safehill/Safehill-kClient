package com.safehill.kclient.models

import java.util.Date

data class SHGenericEncryptedAsset(
    override val globalIdentifier: String,
    override val localIdentifier: String? = null,
    override var creationDate: Date?,
    override var encryptedVersions: Map<SHAssetQuality, SHEncryptedAssetVersion>,
) : SHEncryptedAsset