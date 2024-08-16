package com.safehill.kclient.models.assets

class EncryptedAssetVersion(
    val quality: AssetQuality,
    val encryptedData: ByteArray,
    val encryptedSecret: ByteArray,
    val publicKeyData: ByteArray,
    val publicSignatureData: ByteArray
)