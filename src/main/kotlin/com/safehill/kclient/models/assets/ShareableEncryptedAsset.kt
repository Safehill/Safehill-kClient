package com.safehill.kclient.models.assets

interface ShareableEncryptedAsset {
    val globalIdentifier: AssetGlobalIdentifier
    val sharedVersions: List<ShareableEncryptedAssetVersion>
    val groupId: GroupId
}

data class ShareableEncryptedAssetImpl(
    override val globalIdentifier: AssetGlobalIdentifier,
    override val sharedVersions: List<ShareableEncryptedAssetVersion>,
    override val groupId: GroupId
): ShareableEncryptedAsset
