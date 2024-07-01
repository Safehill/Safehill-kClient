package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.controllers.LocalAssetsStoreController
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAssetImpl
import com.safehill.kclient.models.assets.EncryptedAssetVersionImpl
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.tasks.BackgroundTask
import com.safehill.kclient.utils.ImageResizerInterface
import com.safehill.kcrypto.SafehillCypher
import com.safehill.kcrypto.models.SymmetricKey
import com.safehill.kcrypto.models.bytes

public class UploadOperationImpl(
    val serverProxy: ServerProxy,
    val localAssetsStoreController: LocalAssetsStoreController,
    override val listeners: List<UploadOperationListener>,
    val resizer: ImageResizerInterface,
) : UploadOperation, BackgroundTask {

    // TODO: Persist these on disk
    val outboundQueueItems: MutableList<OutboundQueueItem> = mutableListOf()

    override val user: LocalUser
        get() = serverProxy.requestor

    override suspend fun upload(outboundQueueItem: OutboundQueueItem) {
        val globalIdentifier = outboundQueueItem.globalIdentifier ?: return
        listeners.forEach {
            it.startedEncrypting(
                outboundQueueItem.localAsset.localIdentifier,
                outboundQueueItem.groupId
            )
        }

        val privateSecret = SymmetricKey() //TODO localAssetsStoreController.retrieveCommonEncryptionKey()

        val quality = AssetQuality.LowResolution
        val imageBytes = outboundQueueItem.localAsset.data
        val resizedBytes = resizer.resizeImageIfLarger(imageBytes, quality.dimension, quality.dimension)

        val encryptedData = SafehillCypher.encrypt(resizedBytes, privateSecret)

        val encryptedAssetSecret = user.shareable(privateSecret.bytes, user, user.encryptionSalt)

        val encryptedAssetVersion = EncryptedAssetVersionImpl(
            quality,
            encryptedData,
            encryptedAssetSecret.ciphertext,
            encryptedAssetSecret.ephemeralPublicKeyData,
            encryptedAssetSecret.signature
        )
        val encryptedVersions = mapOf(AssetQuality.LowResolution to encryptedAssetVersion)

        val encryptedAsset = EncryptedAssetImpl(outboundQueueItem.globalIdentifier, outboundQueueItem.localAsset.localIdentifier, outboundQueueItem.localAsset.createdAt, encryptedVersions)

        listeners.forEach {
            it.finishedEncrypting(
                outboundQueueItem.localAsset.localIdentifier,
                outboundQueueItem.groupId
            )
        }

        listeners.forEach {
            it.startedUploading(
                outboundQueueItem.localAsset.localIdentifier,
                outboundQueueItem.groupId
            )
        }

        // 2. Create server asset with the details
        //      2.1 If already exists and any recipients call this.share(), otherwise end early
        val serverAssets = serverProxy.create(listOf(encryptedAsset), outboundQueueItem.groupId, listOf(AssetQuality.LowResolution))

        // 3. Upload encrypted Data to S3

        for (index in serverAssets.indices) {
            serverProxy.upload(serverAssets[index], encryptedAsset, listOf(AssetQuality.LowResolution))
        }

        listeners.forEach {
            it.finishedUploading(
                outboundQueueItem.localAsset.localIdentifier,
                globalIdentifier,
                outboundQueueItem.groupId
            )
        }

        if (outboundQueueItem.recipients.isNotEmpty()) {
            val shareQueueItem = OutboundQueueItem(
                OutboundQueueItem.OperationType.Share,
                outboundQueueItem.localAsset,
                globalIdentifier,
                outboundQueueItem.groupId,
                outboundQueueItem.recipients
            )
            this.share(shareQueueItem)
        }
    }

    override suspend fun share(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.startedSharing(
                outboundQueueItem.localAsset.localIdentifier,
                outboundQueueItem.globalIdentifier!!,
                outboundQueueItem.groupId,
                outboundQueueItem.recipients
            )
        }

        // 1. Encrypt the asset for all recipients

        // 2. Call serverProxy.share() with these encryption details

        listeners.forEach {
            it.finishedSharing(
                outboundQueueItem.localAsset.localIdentifier,
                outboundQueueItem.globalIdentifier!!,
                outboundQueueItem.groupId,
                outboundQueueItem.recipients
            )
        }
    }

    override suspend fun run() {
        while (outboundQueueItems.isNotEmpty()) {
            val queueItem = outboundQueueItems.removeFirst()
            when (queueItem.operationType) {
                OutboundQueueItem.OperationType.Upload -> {
                    this.upload(queueItem)
                }
                OutboundQueueItem.OperationType.Share -> {
                    this.share(queueItem)
                }
            }
        }
    }

}