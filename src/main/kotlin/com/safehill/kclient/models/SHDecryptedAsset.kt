package com.safehill.kclient.models

import java.util.Date

interface SHDecryptedAsset : SHRemoteAssetIdentifiable {
    override var localIdentifier: String?
    var decryptedVersions: MutableMap<SHAssetQuality, ByteArray>
    val creationDate: Date?
}