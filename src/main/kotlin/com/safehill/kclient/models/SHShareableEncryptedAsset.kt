package com.safehill.kclient.models

import com.safehill.kclient.api.AssetGlobalIdentifier

interface SHShareableEncryptedAsset {
    val globalIdentifier: AssetGlobalIdentifier
    val sharedVersions: List<SHShareableEncryptedAssetVersion>
    val groupId: String
}