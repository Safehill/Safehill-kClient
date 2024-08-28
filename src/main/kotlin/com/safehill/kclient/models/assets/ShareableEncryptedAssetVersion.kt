package com.safehill.kclient.models.assets

import com.safehill.kclient.models.users.UserIdentifier

interface ShareableEncryptedAssetVersion {
    val quality: AssetQuality
    val userPublicIdentifier: UserIdentifier
    val encryptedSecret: ByteArray
    val ephemeralPublicKey: ByteArray
    val publicSignature: ByteArray
}

data class ShareableEncryptedAssetVersionImpl (
    override val quality: AssetQuality,
    override val userPublicIdentifier: UserIdentifier,
    override val encryptedSecret: ByteArray,
    override val ephemeralPublicKey: ByteArray,
    override val publicSignature: ByteArray
): ShareableEncryptedAssetVersion {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShareableEncryptedAssetVersionImpl

        if (quality != other.quality) return false
        if (userPublicIdentifier != other.userPublicIdentifier) return false

        return true
    }

    override fun hashCode(): Int {
        var result = quality.hashCode()
        result = 31 * result + userPublicIdentifier.hashCode()
        return result
    }
}
