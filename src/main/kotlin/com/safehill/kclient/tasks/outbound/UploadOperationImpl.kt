package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.controllers.LocalAssetsStoreController
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.LocalAsset
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kcrypto.models.SymmetricKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.UUID

class UploadOperationImpl(
    val serverProxy: ServerProxy,
    override val listeners: List<UploadOperationListener>,
    val encrypter: AssetEncrypterInterface,
) : UploadOperation {

    // TODO: Persist these on disk
    private val outboundQueueItems: Channel<OutboundQueueItem> = Channel(Channel.UNLIMITED)
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override val user: LocalUser
        get() = serverProxy.requestor

    override suspend fun enqueueUpload(
        localAsset: LocalAsset,
        assetQualities: Array<AssetQuality>,
        groupId: GroupId,
        recipients: List<ServerUser>
    ) {
        scope.launch {
            val globalIdentifier = UUID.randomUUID().toString()
            assetQualities.forEach {
                outboundQueueItems.send(
                    OutboundQueueItem(
                        OutboundQueueItem.OperationType.Upload,
                        it,
                        localAsset,
                        globalIdentifier,
                        groupId,
                        recipients
                    )
                )
            }
        }
    }

    override fun enqueueShare(
        localAsset: LocalAsset,
        assetQualities: Array<AssetQuality>,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        recipients: List<ServerUser>
    ) {
        TODO("Not yet implemented")
    }

    suspend fun upload(outboundQueueItem: OutboundQueueItem) {
        try {
            val globalIdentifier = outboundQueueItem.globalIdentifier ?: return

            // *******encrypting*******
            notifyListenersStartedEncrypting(outboundQueueItem)


            val encryptedAsset = encrypter.encryptedAsset(outboundQueueItem, user)

            notifyListenersFinishedEncrypting(outboundQueueItem)
            // #######encrypting#######

            // *******uploading********
            notifyListenersStartedUploading(outboundQueueItem)

            val serverAssets = createServerAssets(outboundQueueItem, encryptedAsset)

            uploadEncryptedDataToS3(serverAssets, encryptedAsset, outboundQueueItem.assetQuality)

            notifyListenersFinishedUploading(outboundQueueItem, globalIdentifier)
            // #######uploading########

            shareIfRecipientsExist(outboundQueueItem, globalIdentifier)

        } catch (exception: Exception) {
            handleUploadException(outboundQueueItem, exception)
        }
    }

    private fun notifyListenersStartedEncrypting(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.startedEncrypting(
                outboundQueueItem.localAsset.localIdentifier,
                outboundQueueItem.groupId
            )
        }
    }

    private fun notifyListenersFinishedEncrypting(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.finishedEncrypting(
                outboundQueueItem.localAsset.localIdentifier,
                outboundQueueItem.groupId
            )
        }
    }

    private fun notifyListenersStartedUploading(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.startedUploading(
                outboundQueueItem.localAsset.localIdentifier,
                outboundQueueItem.groupId
            )
        }
    }

    private fun notifyListenersFinishedUploading(outboundQueueItem: OutboundQueueItem, globalIdentifier: String) {
        listeners.forEach {
            it.finishedUploading(
                outboundQueueItem.localAsset.localIdentifier,
                globalIdentifier,
                outboundQueueItem.groupId
            )
        }
    }

    private suspend fun createServerAssets(
        outboundQueueItem: OutboundQueueItem,
        encryptedAsset: EncryptedAsset,
    ): List<AssetOutputDTO> {
        return serverProxy.create(
            listOf(encryptedAsset),
            outboundQueueItem.groupId,
            listOf(outboundQueueItem.assetQuality)
        )
    }

    private suspend fun uploadEncryptedDataToS3(
        serverAssets: List<AssetOutputDTO>,
        encryptedAsset: EncryptedAsset,
        quality: AssetQuality
    ) {
        for (index in serverAssets.indices) {
            serverProxy.upload(
                serverAssets[index],
                encryptedAsset,
                listOf(quality)
            )
        }
    }

    private suspend fun shareIfRecipientsExist(outboundQueueItem: OutboundQueueItem, globalIdentifier: String) {
        if (outboundQueueItem.recipients.isNotEmpty()) {
            val shareQueueItem = OutboundQueueItem(
                OutboundQueueItem.OperationType.Share,
                outboundQueueItem.assetQuality,
                outboundQueueItem.localAsset,
                globalIdentifier,
                outboundQueueItem.groupId,
                outboundQueueItem.recipients
            )
            this.share(shareQueueItem)
        }
    }

    private fun handleUploadException(outboundQueueItem: OutboundQueueItem, exception: Exception) {
        println(exception.localizedMessage)
        // TODO: Implement exception handling logic, such as retrying the upload
        // Move the item to the end of the queue and reduce the number of remaining retries
    }

    suspend fun share(outboundQueueItem: OutboundQueueItem) {
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

    override fun stop() {
        scope.cancel()
    }

    override suspend fun run() {
        for(queueItem in outboundQueueItems) {
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