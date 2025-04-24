package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.SafehillCypher
import com.safehill.kclient.models.SymmetricKey
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetVersion
import com.safehill.kclient.models.assets.LocalAsset
import com.safehill.kclient.models.bytes
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.utils.ImageResizerInterface

class AssetEncrypter(
    private val resizer: ImageResizerInterface,
    private val localAssetGetter: LocalAssetGetter
) {
    fun encryptedAsset(
        outboundQueueItem: OutboundQueueItem,
        user: LocalUser
    ): EncryptedAsset {
        val sharedSecret = SymmetricKey()
        val encryptedAssetSecret = user.shareable(
            data = sharedSecret.bytes,
            with = user,
            protocolSalt = user.encryptionSalt
        )
        val localAsset = localAssetGetter.getLocalAsset(outboundQueueItem.localIdentifier)

        val encryptedVersions = outboundQueueItem.assetQualities.associateWith { quality ->
            val imageBytes = localAsset.data
            val resizedBytes =
                resizer.resizeImageIfLarger(imageBytes, quality.dimension, quality.dimension)

            val encryptedData = SafehillCypher.encrypt(resizedBytes, sharedSecret)

            EncryptedAssetVersion(
                quality = quality,
                encryptedData = encryptedData,
                encryptedSecret = encryptedAssetSecret.ciphertext,
                publicKeyData = encryptedAssetSecret.ephemeralPublicKeyData,
                publicSignatureData = encryptedAssetSecret.signature
            )
        }

        return EncryptedAsset(
            globalIdentifier = outboundQueueItem.globalIdentifier,
            localIdentifier = localAsset.localIdentifier,
            creationDate = localAsset.createdAt,
            encryptedVersions = encryptedVersions
        )
    }

}


interface LocalAssetGetter {
    fun getLocalAsset(localIdentifier: AssetLocalIdentifier): LocalAsset
}