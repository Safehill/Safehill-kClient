package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.tasks.outbound.model.UploadRequest

class UploadListenersRegistry(
    private val listeners: MutableList<UploadOperationListener>
) {

    fun notifyOfEnqueuing(
        outboundQueueItem: OutboundQueueItem
    ) {
        listeners.forEach {
            it.enqueued(outboundQueueItem)
        }
    }

    fun notifyListenersStartedEncrypting(uploadRequest: UploadRequest) {
        listeners.forEach {
            it.startedEncrypting(
                uploadRequest.localIdentifier,
                uploadRequest.groupId
            )
        }
    }

    fun notifyListenersFinishedEncrypting(request: UploadRequest) {
        listeners.forEach {
            it.finishedEncrypting(
                request.localIdentifier,
                request.groupId
            )
        }
    }

    fun notifyListenersStartedUploading(request: UploadRequest) {
        listeners.forEach {
            it.startedUploading(
                request.localIdentifier,
                request.groupId
            )
        }
    }

    fun notifyListenersFinishedUploading(
        request: UploadRequest,
    ) {
        listeners.forEach {
            it.finishedUploading(
                localIdentifier = request.localIdentifier,
                globalIdentifier = request.globalIdentifier,
                groupId = request.groupId
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