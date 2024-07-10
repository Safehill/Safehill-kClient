package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.controllers.LocalAssetsStoreController
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetImpl
import com.safehill.kclient.models.assets.EncryptedAssetVersionImpl
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.utils.ImageResizerInterface
import com.safehill.kcrypto.SafehillCypher
import com.safehill.kcrypto.models.SymmetricKey
import com.safehill.kcrypto.models.bytes

class AssetEncrypter(
    private val resizer: ImageResizerInterface,
    private val localAssetsStoreController: LocalAssetsStoreController
): AssetEncrypterInterface {
    override suspend fun encryptedAsset(
        outboundQueueItem: OutboundQueueItem,
        user: LocalUser
    ): EncryptedAsset {
        requireNotNull(outboundQueueItem.globalIdentifier)
        val privateSecret = retrieveCommonEncryptionKey(outboundQueueItem.globalIdentifier) // Retrieve the common encryption key

        val quality = outboundQueueItem.assetQuality
        val imageBytes = outboundQueueItem.localAsset.data
        val resizedBytes =
            resizer.resizeImageIfLarger(imageBytes, quality.dimension, quality.dimension)

        val encryptedData = SafehillCypher.encrypt(resizedBytes, privateSecret)

        val encryptedAssetSecret = user.shareable(privateSecret.bytes, user, user.encryptionSalt)

        val encryptedAssetVersion = EncryptedAssetVersionImpl(
            quality,
            encryptedData,
            encryptedAssetSecret.ciphertext,
            encryptedAssetSecret.ephemeralPublicKeyData,
            encryptedAssetSecret.signature
        )
        val encryptedVersions = mapOf(quality to encryptedAssetVersion)

        return EncryptedAssetImpl(
            outboundQueueItem.globalIdentifier,
            outboundQueueItem.localAsset.localIdentifier,
            outboundQueueItem.localAsset.createdAt,
            encryptedVersions
        )
    }

    private suspend fun retrieveCommonEncryptionKey(globalIdentifier: AssetGlobalIdentifier): SymmetricKey {
        var symmetricKey = localAssetsStoreController.encryptionKey(globalIdentifier)
        if (symmetricKey != null) return symmetricKey
        symmetricKey = SymmetricKey()
        localAssetsStoreController.saveEncryptionKey(globalIdentifier, symmetricKey)
        return symmetricKey
    }
}