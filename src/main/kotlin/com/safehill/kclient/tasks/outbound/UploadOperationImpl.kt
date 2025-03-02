package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.LocalAsset
import com.safehill.kclient.models.assets.ShareableEncryptedAssetImpl
import com.safehill.kclient.models.assets.ShareableEncryptedAssetVersionImpl
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.exceptions.SafehillError
import com.safehill.kcrypto.models.ShareablePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

//todo remove this coupling from android and client.
interface ThreadStateRegistryInterface {
    suspend fun upsertThreadStates(threadDtos: List<ConversationThreadOutputDTO>)
}

class UploadOperationImpl(
    val serverProxy: ServerProxy,
    override val listeners: MutableList<UploadOperationListener>,
    private val encrypter: AssetEncrypterInterface,
    private val outboundQueueItemManager: OutboundQueueItemManagerInterface,
) : UploadOperation {

    private val outboundQueueItems: Channel<OutboundQueueItem> = Channel(Channel.UNLIMITED)
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override val user: LocalUser
        get() = serverProxy.requestor

    override suspend fun enqueueUpload(
        localAsset: LocalAsset,
        assetQualities: Array<AssetQuality>,
        groupId: GroupId,
        recipients: List<ServerUser>,
        uri: String?,
        threadId: String?
    ) {
        scope.launch {
            val globalIdentifier = UUID.randomUUID().toString()
            assetQualities.forEach {
                val item = OutboundQueueItem(
                    operationType = OutboundQueueItem.OperationType.Upload,
                    assetQuality = it,
                    localAsset = localAsset,
                    globalIdentifier = globalIdentifier,
                    groupId = groupId,
                    recipients = recipients,
                    uri = uri,
                    threadId = threadId
                )
                outboundQueueItems.send(item)
                outboundQueueItemManager.addOutboundQueueItem(item)
            }
        }
    }

    override fun enqueueShare(
        localAsset: LocalAsset,
        assetQualities: Array<AssetQuality>,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        recipients: List<ServerUser>,
        threadId: String?
    ) {
        scope.launch {
            assetQualities.forEach {
                enqueueShareItem(
                    assetQuality = it,
                    localAsset = localAsset,
                    globalIdentifier = globalIdentifier,
                    groupId = groupId,
                    recipients = recipients,
                    threadId = threadId
                )
            }
        }
    }

    private suspend fun shareIfRecipientsExist(
        outboundQueueItem: OutboundQueueItem,
        globalIdentifier: String
    ) {
        if (outboundQueueItem.recipients.isNotEmpty()) {
            scope.launch {
                enqueueShareItem(
                    assetQuality = outboundQueueItem.assetQuality,
                    localAsset = outboundQueueItem.localAsset,
                    globalIdentifier = globalIdentifier,
                    groupId = outboundQueueItem.groupId,
                    recipients = outboundQueueItem.recipients,
                    threadId = outboundQueueItem.threadId
                )
            }
        }
    }

    private suspend fun enqueueShareItem(
        assetQuality: AssetQuality,
        localAsset: LocalAsset?,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        recipients: List<ServerUser>,
        threadId: String?
    ) {
        val item = OutboundQueueItem(
            operationType = OutboundQueueItem.OperationType.Share,
            assetQuality = assetQuality,
            localAsset = localAsset,
            globalIdentifier = globalIdentifier,
            groupId = groupId,
            recipients = recipients,
            uri = null,
            threadId = threadId
        )
        outboundQueueItems.send(item)
        outboundQueueItemManager.addOutboundQueueItem(item)
    }

    private suspend fun upload(outboundQueueItem: OutboundQueueItem) {
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
            outboundQueueItem.localAsset?.localIdentifier?.let { localAsset ->
                it.startedEncrypting(
                    localAsset,
                    outboundQueueItem.groupId,
                    outboundQueueItem.assetQuality
                )
            }
        }
    }

    private fun notifyListenersFinishedEncrypting(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            outboundQueueItem.localAsset?.let { localAsset ->
                it.finishedEncrypting(
                    localAsset.localIdentifier,
                    outboundQueueItem.groupId,
                    outboundQueueItem.assetQuality
                )
            }
        }
    }

    private fun notifyListenersStartedUploading(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            outboundQueueItem.localAsset?.let { localAsset ->
                it.startedUploading(
                    localAsset.localIdentifier,
                    outboundQueueItem.groupId,
                    outboundQueueItem.assetQuality
                )
            }
        }
    }

    private fun notifyListenersFinishedUploading(
        outboundQueueItem: OutboundQueueItem,
        globalIdentifier: String
    ) {
        listeners.forEach {
            outboundQueueItem.localAsset?.let { localAsset ->
                it.finishedUploading(
                    localAsset.localIdentifier,
                    globalIdentifier,
                    outboundQueueItem.groupId,
                    outboundQueueItem.assetQuality
                )
            }
        }
    }

    private fun notifyListenersFailedUploading(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            outboundQueueItem.localAsset?.let { localAsset ->
                it.failedUploading(
                    localAsset.localIdentifier,
                    outboundQueueItem.groupId,
                    outboundQueueItem.assetQuality
                )
            }
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

    private suspend fun handleUploadException(
        outboundQueueItem: OutboundQueueItem,
        exception: Exception
    ) {
        println(exception.localizedMessage)
        when (exception) {
            is SafehillError.ClientError.Conflict -> {
                if (!outboundQueueItem.force) {
                    outboundQueueItem.force = true
                } else {
                    notifyListenersFailedUploading(outboundQueueItem)
                    return
                }
            }

            is SafehillError.ClientError.BadRequest,
            SafehillError.ClientError.Unauthorized,
            SafehillError.ClientError.PaymentRequired -> {
                notifyListenersFailedUploading(outboundQueueItem)
                return
            }
        }
        // Move the item to the end of the queue and reduce the number of remaining retries
        scope.launch {
            outboundQueueItems.send(outboundQueueItem)
            outboundQueueItemManager.addOutboundQueueItem(outboundQueueItem)
        }
    }

    private suspend fun share(outboundQueueItem: OutboundQueueItem) {
        if (outboundQueueItem.globalIdentifier == null) return

        notifyListenersStartedSharing(outboundQueueItem)
        try {
            for (recipient in outboundQueueItem.recipients) {
                val (_, sharablePayload) = encrypter.getSharablePayload(
                    outboundQueueItem,
                    user,
                    recipient
                )

                serverShare(
                    outboundQueueItem,
                    recipient,
                    sharablePayload,
                    outboundQueueItem.threadId!!
                )
            }
        } catch (e: Exception) {
            //TODO better exception handling
            println(e.localizedMessage)
            notifyListenersFailedSharing(outboundQueueItem)
            return
        }

        notifyListenersFinishedSharing(outboundQueueItem)
    }

    private suspend fun serverShare(
        outboundQueueItem: OutboundQueueItem,
        recipient: ServerUser,
        sharablePayload: ShareablePayload,
        threadId: String
    ) {
        serverProxy.share(
            ShareableEncryptedAssetImpl(
                outboundQueueItem.globalIdentifier!!,
                listOf(
                    ShareableEncryptedAssetVersionImpl(
                        outboundQueueItem.assetQuality,
                        recipient.identifier,
                        sharablePayload.ciphertext,
                        sharablePayload.ephemeralPublicKeyData,
                        sharablePayload.signature
                    )
                ),
                outboundQueueItem.groupId
            ),
            threadId
        )
    }

    private fun notifyListenersFinishedSharing(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.finishedSharing(
                outboundQueueItem.localAsset?.localIdentifier,
                outboundQueueItem.globalIdentifier!!,
                outboundQueueItem.groupId,
                outboundQueueItem.recipients
            )
        }
    }

    private fun notifyListenersFailedSharing(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.failedSharing(
                outboundQueueItem.localAsset?.localIdentifier,
                outboundQueueItem.globalIdentifier!!,
                outboundQueueItem.groupId,
                outboundQueueItem.recipients
            )
        }
    }

    private fun notifyListenersStartedSharing(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.startedSharing(
                outboundQueueItem.localAsset?.localIdentifier,
                outboundQueueItem.globalIdentifier!!,
                outboundQueueItem.groupId,
                outboundQueueItem.recipients
            )
        }
    }

    override fun stop() {}

    override suspend fun run() {
        coroutineScope {
            launch {
                outboundQueueItemManager.loadOutboundQueueItems().forEach {
                    outboundQueueItems.send(it)
                }
            }
            launch {
                for (queueItem in outboundQueueItems) {
                    when (queueItem.operationType) {
                        OutboundQueueItem.OperationType.Upload -> {
                            upload(queueItem)
                        }

                        OutboundQueueItem.OperationType.Share -> {
                            share(queueItem)
                        }
                    }
                    outboundQueueItemManager.removeOutboundQueueItem(queueItem)
                }
            }
        }
    }

}