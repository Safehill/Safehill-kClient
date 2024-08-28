package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.controllers.LocalAssetsStoreController
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetVersion
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.utils.ImageResizerInterface
import com.safehill.kclient.SafehillCypher
import com.safehill.kclient.models.ShareablePayload
import com.safehill.kclient.models.SymmetricKey
import com.safehill.kclient.models.bytes
import com.safehill.kclient.models.users.ServerUser

class AssetEncrypter(
    private val resizer: ImageResizerInterface,
    private val localAssetsStoreController: LocalAssetsStoreController
) : AssetEncrypterInterface {
    override suspend fun encryptedAsset(
        outboundQueueItem: OutboundQueueItem,
        user: LocalUser,
        recipient: ServerUser
    ): EncryptedAsset {
        requireNotNull(outboundQueueItem.globalIdentifier)
        requireNotNull(outboundQueueItem.localAsset)
        val (privateSecret, encryptedAssetSecret) = getSharablePayload(outboundQueueItem, user, recipient)

        val quality = outboundQueueItem.assetQuality
        val imageBytes = outboundQueueItem.localAsset.data
        val resizedBytes =
            resizer.resizeImageIfLarger(imageBytes, quality.dimension, quality.dimension)

        val encryptedData = SafehillCypher.encrypt(resizedBytes, privateSecret)

        val encryptedAssetVersion = EncryptedAssetVersion(
            quality,
            encryptedData,
            encryptedAssetSecret.ciphertext,
            encryptedAssetSecret.ephemeralPublicKeyData,
            encryptedAssetSecret.signature
        )
        val encryptedVersions = mapOf(quality to encryptedAssetVersion)

        return EncryptedAsset(
            outboundQueueItem.globalIdentifier,
            outboundQueueItem.localAsset.localIdentifier,
            outboundQueueItem.localAsset.createdAt,
            encryptedVersions
        )
    }

    override suspend fun getSharablePayload(
        outboundQueueItem: OutboundQueueItem,
        user: LocalUser,
        recipient: ServerUser
    ): Pair<SymmetricKey, ShareablePayload> {
        requireNotNull(outboundQueueItem.globalIdentifier)
        val privateSecret =
            retrieveCommonEncryptionKey(outboundQueueItem.globalIdentifier) // Retrieve the common encryption key

        val encryptedAssetSecret =
            user.shareable(privateSecret.bytes, recipient, user.encryptionSalt)
        return Pair(privateSecret, encryptedAssetSecret)
    }

    private suspend fun retrieveCommonEncryptionKey(globalIdentifier: AssetGlobalIdentifier): SymmetricKey {
        var symmetricKey = localAssetsStoreController.encryptionKey(globalIdentifier)
        if (symmetricKey != null) return symmetricKey
        symmetricKey = SymmetricKey()
        localAssetsStoreController.saveEncryptionKey(globalIdentifier, symmetricKey)
        return symmetricKey
    }
}