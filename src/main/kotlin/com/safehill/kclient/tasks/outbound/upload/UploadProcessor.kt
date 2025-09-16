package com.safehill.kclient.tasks.outbound.upload

import com.safehill.kclient.tasks.outbound.model.UploadExecutor
import com.safehill.kclient.tasks.outbound.model.UploadRequest
import com.safehill.kclient.tasks.outbound.model.UploadState
import com.safehill.kclient.tasks.upload.queue.ItemProcessor
import com.safehill.kclient.util.runCatchingSafe

class UploadProcessor(
    private val uploadExecutor: UploadExecutor,
    private val uploadStates: UploadStates,
    private val uploadOperation: UploadOperation
) : ItemProcessor<UploadRequest> {

    override suspend fun onEnqueued(item: UploadRequest) {
        uploadStates.addItem(item, UploadState.Pending)
    }

    override suspend fun process(item: UploadRequest): Result<Unit> {
        return runCatchingSafe {
            uploadExecutor.execute(item).collect { state ->
                uploadStates.updateState(item.id, state)
            }
            if (item.recipients.isNotEmpty()) {
                uploadOperation.enqueueShare(
                    localIdentifier = item.localIdentifier,
                    globalIdentifier = item.globalIdentifier,
                    assetQualities = item.qualities,
                    groupId = item.groupId,
                    recipientIds = item.recipients,
                    threadId = item.threadId
                )
            }
        }
    }
}