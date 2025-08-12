package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.SafehillCypher
import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.SafehillHash
import com.safehill.kclient.models.SymmetricKey
import com.safehill.kclient.models.assets.AssetFingerPrint
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetVersion
import com.safehill.kclient.models.assets.LocalAsset
import com.safehill.kclient.models.bytes
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.tasks.outbound.embedding.AssetEmbeddings
import com.safehill.kclient.utils.ImageResizerInterface

class AssetEncrypter(
    private val resizer: ImageResizerInterface,
    private val localAssetGetter: LocalAssetGetter,
    private val assetEmbeddings: AssetEmbeddings,
    private val safehillLogger: SafehillLogger
) {

    suspend fun encryptedAsset(
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


        var assetFingerprint: AssetFingerPrint? = null

        val encryptedVersions = outboundQueueItem.assetQualities.associateWith { quality ->
            val resizedBytes = resizer.resizeImageIfLarger(
                localAsset.data,
                quality.dimension,
                quality.dimension
            )
            if (quality == AssetQuality.LowResolution) {
                assetFingerprint = AssetFingerPrint(
                    embeddings = assetEmbeddings.getEmbeddings(resizedBytes),
                    assetHash = SafehillHash.stringDigest(resizedBytes)
                )
            }
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
            encryptedVersions = encryptedVersions,
            fingerPrint = assetFingerprint
        )
    }

}


interface LocalAssetGetter {
    fun getLocalAsset(localIdentifier: AssetLocalIdentifier): LocalAsset
}