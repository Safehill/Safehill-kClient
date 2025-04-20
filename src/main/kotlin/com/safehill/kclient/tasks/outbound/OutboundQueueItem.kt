package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.UserIdentifier

class OutboundQueueItem(
    val operationType: OperationType,
    val assetQualities: List<AssetQuality>,
    val globalIdentifier: AssetGlobalIdentifier,
    val localIdentifier: AssetLocalIdentifier,
    val groupId: GroupId,
    val recipientIds: List<UserIdentifier>,
    val threadId: String?
) {

    enum class OperationType {
        Upload, Share
    }
}