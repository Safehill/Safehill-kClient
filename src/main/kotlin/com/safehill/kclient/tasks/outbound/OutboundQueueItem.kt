package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.ServerUser

class OutboundQueueItem(
    val operationType: OperationType,
    val assetQuality: AssetQuality,
    val globalIdentifier: AssetGlobalIdentifier,
    val localIdentifier: AssetLocalIdentifier,
    val groupId: GroupId,
    val recipients: List<ServerUser>,
    var force: Boolean = false,
    var threadId: String?
) {

    enum class OperationType {
        Upload, Share
    }
}