package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.tasks.BackgroundTask
import java.util.UUID

public class UploadOperationImpl(
    val serverProxy: ServerProxy,
    override val listeners: List<UploadOperationListener>,
) : UploadOperation, BackgroundTask {

    // TODO: Persist these on disk
    val outboundQueueItems: MutableList<OutboundQueueItem> = mutableListOf()

    override val user: LocalUser
        get() = serverProxy.requestor

    override suspend fun upload(outboundQueueItem: OutboundQueueItem) {

        listeners.forEach {
            it.startedEncrypting(
                outboundQueueItem.localAsset.localIdentifier,
                outboundQueueItem.groupId
            )
        }

        // 1. Encrypt data in LocalAsset

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

        val globalIdentifier = UUID.randomUUID().toString()

        // 2. Create server asset with the details
        //      2.1 If already exists and any recipients call this.share(), otherwise end early

        // 3. Upload encrypted Data to S3

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