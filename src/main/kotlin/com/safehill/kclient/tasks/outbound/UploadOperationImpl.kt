package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.ShareableEncryptedAssetImpl
import com.safehill.kclient.models.assets.ShareableEncryptedAssetVersionImpl
import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.models.users.getOrNull
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.exceptions.SafehillError
import com.safehill.kcrypto.models.ShareablePayload
import com.safehill.safehillclient.module.platform.UserModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class UploadOperationImpl(
    val serverProxy: ServerProxy,
    override val listeners: MutableList<UploadOperationListener>,
    private val encrypter: AssetEncrypter,
    private val userModule: UserModule,
    private val userProvider: UserProvider
) : UploadOperation {

    private val outboundQueueItemManager: OutboundQueueItemManagerInterface?
        get() {
            val currentUser = userProvider.getOrNull() ?: return null
            return userModule.getOutboundQueueItemManager(currentUser)
        }

    private val outboundQueueItems: Channel<OutboundQueueItem> = Channel(Channel.UNLIMITED)
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override val user: LocalUser
        get() = serverProxy.requestor

    override fun enqueueUpload(
        localIdentifier: AssetLocalIdentifier,
        assetQualities: Array<AssetQuality>,
        groupId: GroupId,
        recipients: List<ServerUser>,
        threadId: String?
    ) {
        scope.launch {
            val globalIdentifier = UUID.randomUUID().toString()
            assetQualities.forEach {
                val item = OutboundQueueItem(
                    operationType = OutboundQueueItem.OperationType.Upload,
                    assetQuality = it,
                    localIdentifier = localIdentifier,
                    globalIdentifier = globalIdentifier,
                    groupId = groupId,
                    recipients = recipients,
                    threadId = threadId
                )
                sendToChannelAndNotifyListeners(item)
                outboundQueueItemManager?.addOutboundQueueItem(item)
            }
        }
    }

    override fun enqueueShare(
        assetQualities: Array<AssetQuality>,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        localIdentifier: AssetLocalIdentifier,
        recipients: List<ServerUser>,
        threadId: String?
    ) {
        scope.launch {
            assetQualities.forEach {
                enqueueShareItem(
                    assetQuality = it,
                    globalIdentifier = globalIdentifier,
                    groupId = groupId,
                    recipients = recipients,
                    threadId = threadId,
                    localIdentifier = localIdentifier
                )
            }
        }
    }

    private suspend fun sendToChannelAndNotifyListeners(outboundQueueItem: OutboundQueueItem) {
        outboundQueueItems.send(outboundQueueItem)
        val threadId = outboundQueueItem.threadId
        if (threadId != null) {
            listeners.forEach {
                it.enqueued(
                    threadId = threadId,
                    localIdentifier = outboundQueueItem.localIdentifier,
                    globalIdentifier = outboundQueueItem.globalIdentifier,
                    groupId = outboundQueueItem.groupId
                )
            }
        }
    }

    private fun shareIfRecipientsExist(
        outboundQueueItem: OutboundQueueItem,
        globalIdentifier: String
    ) {
        if (outboundQueueItem.recipients.isNotEmpty()) {
            scope.launch {
                enqueueShareItem(
                    assetQuality = outboundQueueItem.assetQuality,
                    globalIdentifier = globalIdentifier,
                    groupId = outboundQueueItem.groupId,
                    recipients = outboundQueueItem.recipients,
                    threadId = outboundQueueItem.threadId,
                    localIdentifier = outboundQueueItem.localIdentifier
                )
            }
        }
    }

    private suspend fun enqueueShareItem(
        assetQuality: AssetQuality,
        globalIdentifier: AssetGlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        recipients: List<ServerUser>,
        threadId: String?
    ) {
        val item = OutboundQueueItem(
            operationType = OutboundQueueItem.OperationType.Share,
            globalIdentifier = globalIdentifier,
            groupId = groupId,
            recipients = recipients,
            threadId = threadId,
            assetQuality = assetQuality,
            localIdentifier = localIdentifier,
        )
        sendToChannelAndNotifyListeners(item)
        outboundQueueItemManager?.addOutboundQueueItem(item)
    }

    private suspend fun upload(outboundQueueItem: OutboundQueueItem) {
        try {
            val globalIdentifier = outboundQueueItem.globalIdentifier

            // *******encrypting*******
            notifyListenersStartedEncrypting(outboundQueueItem)

            val encryptedAsset = encrypter.encryptedAsset(
                outboundQueueItem = outboundQueueItem,
                user = user,
                recipient = user
            )

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
                outboundQueueItem.localIdentifier,
                outboundQueueItem.groupId,
                outboundQueueItem.assetQuality
            )
        }
    }

    private fun notifyListenersFinishedEncrypting(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.finishedEncrypting(
                outboundQueueItem.localIdentifier,
                outboundQueueItem.groupId,
                outboundQueueItem.assetQuality
            )
        }
    }

    private fun notifyListenersStartedUploading(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.startedUploading(
                outboundQueueItem.localIdentifier,
                outboundQueueItem.groupId,
                outboundQueueItem.assetQuality
            )
        }
    }

    private fun notifyListenersFinishedUploading(
        outboundQueueItem: OutboundQueueItem,
        globalIdentifier: String
    ) {
        listeners.forEach {
            it.finishedUploading(
                outboundQueueItem.localIdentifier,
                globalIdentifier,
                outboundQueueItem.groupId,
                outboundQueueItem.assetQuality
            )
        }
    }

    private fun notifyListenersFailedUploading(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.failedUploading(
                outboundQueueItem.localIdentifier,
                outboundQueueItem.groupId,
                outboundQueueItem.assetQuality
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

    private fun handleUploadException(
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
            sendToChannelAndNotifyListeners(outboundQueueItem)
            outboundQueueItemManager?.addOutboundQueueItem(outboundQueueItem)
        }
    }

    private suspend fun share(outboundQueueItem: OutboundQueueItem) {
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
                outboundQueueItem.globalIdentifier,
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
                outboundQueueItem.localIdentifier,
                outboundQueueItem.globalIdentifier,
                outboundQueueItem.groupId,
                outboundQueueItem.recipients
            )
        }
    }

    private fun notifyListenersFailedSharing(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.failedSharing(
                outboundQueueItem.localIdentifier,
                outboundQueueItem.globalIdentifier,
                outboundQueueItem.groupId,
                outboundQueueItem.recipients
            )
        }
    }

    private fun notifyListenersStartedSharing(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.startedSharing(
                outboundQueueItem.localIdentifier,
                outboundQueueItem.globalIdentifier,
                outboundQueueItem.groupId,
                outboundQueueItem.recipients
            )
        }
    }

    override fun stop() {}

    override suspend fun run() {
        coroutineScope {
            launch {
                launch {
                    println("ABCD sending ")

                    outboundQueueItemManager?.loadOutboundQueueItems()?.forEach {
                        println("ABCD sending $it")
                        sendToChannelAndNotifyListeners(it)
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
                        outboundQueueItemManager?.removeOutboundQueueItem(queueItem)
                    }
                }
            }.invokeOnCompletion {
                while (true) {
                    outboundQueueItems.tryReceive().getOrNull() ?: break
                }
            }
        }
    }
}