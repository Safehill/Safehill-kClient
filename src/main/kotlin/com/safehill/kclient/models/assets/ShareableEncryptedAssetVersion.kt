package com.safehill.kclient.models.assets

interface ShareableEncryptedAssetVersion {
    val quality: AssetQuality
    val userPublicIdentifier: String
    val encryptedSecret: ByteArray
    val ephemeralPublicKey: ByteArray
    val publicSignature: ByteArray
}
