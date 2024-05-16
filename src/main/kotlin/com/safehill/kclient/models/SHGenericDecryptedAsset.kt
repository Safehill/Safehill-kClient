package com.safehill.kclient.models

import java.util.Date

data class SHGenericDecryptedAsset(
    override val globalIdentifier: String,
    override var localIdentifier: String? = null,
    override var decryptedVersions: MutableMap<SHAssetQuality, ByteArray> = mutableMapOf(),
    override val creationDate: Date? = null
) : SHDecryptedAsset