package com.safehill.kclient.models.assets

class EncryptedAssetVersionImpl(
    override val quality: AssetQuality,
    override val encryptedData: ByteArray,
    override val encryptedSecret: ByteArray,
    override val publicKeyData: ByteArray,
    override val publicSignatureData: ByteArray
) : EncryptedAssetVersion

