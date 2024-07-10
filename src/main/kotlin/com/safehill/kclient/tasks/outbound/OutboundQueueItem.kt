package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.LocalAsset
import com.safehill.kclient.models.users.ServerUser

class OutboundQueueItem(
    val operationType: OperationType,
    val assetQuality: AssetQuality,
    val localAsset: LocalAsset,
    val globalIdentifier: AssetGlobalIdentifier?,
    val groupId: GroupId,
    val recipients: List<ServerUser>
) {

    enum class OperationType {
        Upload, Share
    }
}