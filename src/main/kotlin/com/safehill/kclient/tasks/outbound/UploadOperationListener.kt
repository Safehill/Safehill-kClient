package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.ServerUser

public interface UploadOperationListener {

    fun startedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId
    )

    fun finishedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId
    )

    fun failedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId
    )

    fun startedUploading(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId
    )

    fun finishedUploading(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId
    )

    fun failedUploading(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId
    )

    fun startedSharing(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    )

    fun finishedSharing(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    )

    fun failedSharing(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    )

}