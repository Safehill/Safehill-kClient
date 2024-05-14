package com.safehill.kclient.models.assets

interface ShareableEncryptedAsset {
    val globalIdentifier: AssetGlobalIdentifier
    val sharedVersions: List<ShareableEncryptedAssetVersion>
    val groupId: GroupId
}