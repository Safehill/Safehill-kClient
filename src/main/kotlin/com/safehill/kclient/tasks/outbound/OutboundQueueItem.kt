package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.UserIdentifier

data class OutboundQueueItem(
    val operationType: OperationType,
    val assetQualities: List<AssetQuality>,
    val globalIdentifier: AssetGlobalIdentifier,
    val localIdentifier: AssetLocalIdentifier,
    val groupId: GroupId,
    val operationState: OperationState,
    val recipientIds: List<UserIdentifier>,
    val threadId: String?
) {

    enum class OperationType {
        Upload, Share
    }

    sealed class OperationState {
        data class Failed(val uploadFailure: UploadFailure) : OperationState()
        data object Enqueued : OperationState()
    }
}

enum class UploadFailure {
    ENCRYPTION,
    UPLOAD,
    SHARING
}