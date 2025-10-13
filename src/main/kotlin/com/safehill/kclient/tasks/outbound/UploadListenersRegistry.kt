package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.users.ServerUser

class UploadListenersRegistry(
    val listeners: MutableList<UploadOperationListener>
) {

    fun notifyOfEnqueuing(
        outboundQueueItem: OutboundQueueItem
    ) {
        listeners.forEach {
            it.enqueued(outboundQueueItem)
        }
    }

    fun notifyListenersStartedEncrypting(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.startedEncrypting(
                outboundQueueItem.localIdentifier,
                outboundQueueItem.groupId
            )
        }
    }

    fun notifyListenersFinishedEncrypting(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.finishedEncrypting(
                outboundQueueItem.localIdentifier,
                outboundQueueItem.groupId
            )
        }
    }

    fun notifyListenersStartedUploading(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.startedUploading(
                outboundQueueItem.localIdentifier,
                outboundQueueItem.groupId
            )
        }
    }

    fun notifyListenersFinishedUploading(
        outboundQueueItem: OutboundQueueItem,
    ) {
        listeners.forEach {
            it.finishedUploading(
                localIdentifier = outboundQueueItem.localIdentifier,
                globalIdentifier = outboundQueueItem.globalIdentifier,
                groupId = outboundQueueItem.groupId
            )
        }
    }

    fun notifyListenersFailedUploading(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.failedUploading(
                globalIdentifier = outboundQueueItem.globalIdentifier,
                localIdentifier = outboundQueueItem.localIdentifier,
                groupId = outboundQueueItem.groupId
            )
        }
    }

    fun notifyListenersFinishedSharing(
        outboundQueueItem: OutboundQueueItem,
        recipients: List<ServerUser>
    ) {
        listeners.forEach {
            it.finishedSharing(
                localIdentifier = outboundQueueItem.localIdentifier,
                globalIdentifier = outboundQueueItem.globalIdentifier,
                groupId = outboundQueueItem.groupId,
                users = recipients
            )
        }
    }

    fun notifyListenersFailedSharing(
        outboundQueueItem: OutboundQueueItem
    ) {
        listeners.forEach {
            it.failedSharing(
                localIdentifier = outboundQueueItem.localIdentifier,
                globalIdentifier = outboundQueueItem.globalIdentifier,
                groupId = outboundQueueItem.groupId,
                users = listOf()
            )
        }
    }

    fun notifyListenersStartedSharing(
        outboundQueueItem: OutboundQueueItem,
        recipients: List<ServerUser>
    ) {
        listeners.forEach {
            it.startedSharing(
                localIdentifier = outboundQueueItem.localIdentifier,
                globalIdentifier = outboundQueueItem.globalIdentifier,
                groupId = outboundQueueItem.groupId,
                users = recipients
            )
        }
    }

}