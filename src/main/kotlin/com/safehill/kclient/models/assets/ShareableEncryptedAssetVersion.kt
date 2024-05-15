package com.safehill.kclient.models.assets

import com.safehill.kclient.models.users.UserIdentifier

interface ShareableEncryptedAssetVersion {
    val quality: AssetQuality
    val userPublicIdentifier: UserIdentifier
    val encryptedSecret: ByteArray
    val ephemeralPublicKey: ByteArray
    val publicSignature: ByteArray
}
