package com.safehill.kclient.models.assets

import com.safehill.kclient.models.users.UserIdentifier

data class ShareableEncryptedAsset(
    val globalIdentifier: AssetGlobalIdentifier,
    val sharedVersions: List<ShareableEncryptedAssetVersion>,
    val groupId: GroupId
)

data class ShareableEncryptedAssetVersion(
    val quality: AssetQuality,
    val userPublicIdentifier: UserIdentifier,
    val encryptedSecret: ByteArray,
    val ephemeralPublicKey: ByteArray,
    val publicSignature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShareableEncryptedAssetVersion

        if (quality != other.quality) return false
        if (userPublicIdentifier != other.userPublicIdentifier) return false
        if (!encryptedSecret.contentEquals(other.encryptedSecret)) return false
        if (!ephemeralPublicKey.contentEquals(other.ephemeralPublicKey)) return false
        if (!publicSignature.contentEquals(other.publicSignature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = quality.hashCode()
        result = 31 * result + userPublicIdentifier.hashCode()
        result = 31 * result + encryptedSecret.contentHashCode()
        result = 31 * result + ephemeralPublicKey.contentHashCode()
        result = 31 * result + publicSignature.contentHashCode()
        return result
    }
}