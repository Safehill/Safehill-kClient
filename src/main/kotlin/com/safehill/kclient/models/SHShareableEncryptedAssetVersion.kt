package com.safehill.kclient.models

interface SHShareableEncryptedAssetVersion {
    val quality: SHAssetQuality
    val userPublicIdentifier: String
    val encryptedSecret: ByteArray
    val ephemeralPublicKey: ByteArray
    val publicSignature: ByteArray
}
