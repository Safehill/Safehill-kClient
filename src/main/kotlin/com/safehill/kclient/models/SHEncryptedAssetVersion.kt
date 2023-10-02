package com.safehill.kclient.models

interface SHEncryptedAssetVersion {
    val quality: SHAssetQuality
    val encryptedData: ByteArray
    val encryptedSecret: ByteArray
    val publicKeyData: ByteArray
    val publicSignatureData: ByteArray
}
