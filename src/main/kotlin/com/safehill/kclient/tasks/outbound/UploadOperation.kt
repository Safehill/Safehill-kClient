package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.LocalAsset
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.tasks.BackgroundTask

interface UploadOperation: BackgroundTask {

    val listeners: List<UploadOperationListener>

    val user: LocalUser

    suspend fun enqueueUpload(localAsset: LocalAsset, assetQualities: Array<AssetQuality> = AssetQuality.entries.toTypedArray(), groupId: GroupId, recipients: List<ServerUser> = listOf())

    fun enqueueShare(localAsset: LocalAsset, assetQualities: Array<AssetQuality>, globalIdentifier: AssetGlobalIdentifier, groupId: GroupId, recipients: List<ServerUser>)

    fun stop()
}
