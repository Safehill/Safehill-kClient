package com.safehill.kclient.models.assets

import com.safehill.kclient.api.AssetGlobalIdentifier

interface ShareableEncryptedAsset {
    val globalIdentifier: AssetGlobalIdentifier
    val sharedVersions: List<ShareableEncryptedAssetVersion>
    val groupId: String
}