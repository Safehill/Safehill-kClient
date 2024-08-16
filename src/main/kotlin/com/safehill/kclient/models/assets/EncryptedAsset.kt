package com.safehill.kclient.models.assets

import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.ShareablePayload
import java.time.Instant

data class EncryptedAsset(
    val globalIdentifier: AssetGlobalIdentifier,
    val localIdentifier: AssetLocalIdentifier,
    val creationDate: Instant?,
    val encryptedVersions: Map<AssetQuality, EncryptedAssetVersion>
)

fun EncryptedAsset.toDecryptedAsset(
    senderUser: ServerUser,
    currentUser: LocalUser
): DecryptedAsset {
    val decryptedVersions = this.encryptedVersions.mapValues { (_, encryptedVersion) ->
        val sharedSecret = encryptedVersion.toShareablePayload(
            currentUser = currentUser
        )
        currentUser.decrypted(
            data = encryptedVersion.encryptedData,
            encryptedSecret = sharedSecret,
            receivedFrom = senderUser,
            protocolSalt = currentUser.encryptionSalt
        )
    }
    return DecryptedAsset(
        globalIdentifier = this.globalIdentifier,
        localIdentifier = this.localIdentifier,
        creationDate = this.creationDate,
        decryptedVersions = decryptedVersions
    )
}

private fun EncryptedAssetVersion.toShareablePayload(
    currentUser: LocalUser
) = ShareablePayload(
    ephemeralPublicKeyData = this.publicKeyData,
    ciphertext = this.encryptedSecret,
    signature = this.publicSignatureData,
    recipient = currentUser
)
