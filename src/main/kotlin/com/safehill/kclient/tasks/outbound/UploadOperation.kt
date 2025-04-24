package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.tasks.BackgroundTask

interface UploadOperation : BackgroundTask {

    val listeners: MutableList<UploadOperationListener>

    val user: LocalUser

    fun enqueueUpload(
        localIdentifier: AssetLocalIdentifier,
        assetQualities: List<AssetQuality> = AssetQuality.entries,
        groupId: GroupId,
        recipientIds: List<UserIdentifier>,
        threadId: String? = null
    )

    fun enqueueShare(
        assetQualities: List<AssetQuality>,
        globalIdentifier: AssetGlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        recipientIds: List<UserIdentifier>,
        threadId: String? = null
    )

    fun stop()
}
