package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.SafehillCypher
import com.safehill.kclient.models.SymmetricKey
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.EncryptedAssetVersion
import com.safehill.kclient.models.assets.LocalAsset
import com.safehill.kclient.models.bytes
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.network.local.EncryptionHelper
import com.safehill.kclient.utils.ImageResizerInterface
import com.safehill.kcrypto.models.ShareablePayload
import com.safehill.safehillclient.module.platform.UserModule

class AssetEncrypter(
    private val resizer: ImageResizerInterface,
    private val userModule: UserModule,
    private val userProvider: UserProvider,
    private val localAssetGetter: LocalAssetGetter
) {

    private val encryptionHelper: EncryptionHelper
        get() {
            val currentUser = userProvider.get()
            return userModule.getEncryptionHelper(currentUser)
        }

    suspend fun encryptedAsset(
        outboundQueueItem: OutboundQueueItem,
        user: LocalUser,
        recipient: ServerUser
    ): EncryptedAsset {
        requireNotNull(outboundQueueItem.globalIdentifier)
        val (privateSecret, encryptedAssetSecret) = getSharablePayload(
            outboundQueueItem,
            user,
            recipient
        )

        val quality = outboundQueueItem.assetQuality
        val localAsset = localAssetGetter.getLocalAsset(outboundQueueItem.localIdentifier)
        val imageBytes = localAsset.data
        val resizedBytes =
            resizer.resizeImageIfLarger(imageBytes, quality.dimension, quality.dimension)

        val encryptedData = SafehillCypher.encrypt(resizedBytes, privateSecret)

        val encryptedAssetVersion = EncryptedAssetVersion(
            quality = quality,
            encryptedData = encryptedData,
            encryptedSecret = encryptedAssetSecret.ciphertext,
            publicKeyData = encryptedAssetSecret.ephemeralPublicKeyData,
            publicSignatureData = encryptedAssetSecret.signature
        )
        val encryptedVersions = mapOf(quality to encryptedAssetVersion)

        return EncryptedAsset(
            globalIdentifier = outboundQueueItem.globalIdentifier,
            localIdentifier = localAsset.localIdentifier,
            creationDate = localAsset.createdAt,
            encryptedVersions = encryptedVersions
        )
    }

    suspend fun getSharablePayload(
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
        var symmetricKey = encryptionHelper.getEncryptionKey(globalIdentifier)
        if (symmetricKey != null) return symmetricKey
        symmetricKey = SymmetricKey()
        encryptionHelper.saveEncryptionKey(globalIdentifier, symmetricKey)
        return symmetricKey
    }
}


interface LocalAssetGetter {
    fun getLocalAsset(localIdentifier: AssetLocalIdentifier): LocalAsset
}