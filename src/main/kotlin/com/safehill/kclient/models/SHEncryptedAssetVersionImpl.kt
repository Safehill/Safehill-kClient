package com.safehill.kclient.models

class SHEncryptedAssetVersionImpl(
    override val quality: SHAssetQuality,
    override val encryptedData: ByteArray,
    override val encryptedSecret: ByteArray,
    override val publicKeyData: ByteArray,
    override val publicSignatureData: ByteArray
) : SHEncryptedAssetVersion

