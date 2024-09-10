package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.ServerUser

public interface UploadOperationListener {

    fun startedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        assetQuality: AssetQuality
    )

    fun finishedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        assetQuality: AssetQuality
    )

    fun failedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        assetQuality: AssetQuality
    )

    fun startedUploading(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        assetQuality: AssetQuality
    )

    fun finishedUploading(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        assetQuality: AssetQuality
    )

    fun failedUploading(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        assetQuality: AssetQuality
    )

    fun startedSharing(
        localIdentifier: AssetLocalIdentifier?,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    )

    fun finishedSharing(
        localIdentifier: AssetLocalIdentifier?,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    )

    fun failedSharing(
        localIdentifier: AssetLocalIdentifier?,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    )

}

public abstract class UploadOperationListenerAbstract: UploadOperationListener {
    override fun startedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        assetQuality: AssetQuality
    ) {}

    override fun finishedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        assetQuality: AssetQuality
    ) {}

    override fun failedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        assetQuality: AssetQuality
    ) {}

    override fun startedUploading(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        assetQuality: AssetQuality
    ) {}

    override fun finishedUploading(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        assetQuality: AssetQuality
    ) {}

    override fun failedUploading(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        assetQuality: AssetQuality
    ) {}

    override fun startedSharing(
        localIdentifier: AssetLocalIdentifier?,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    ) {}

    override fun finishedSharing(
        localIdentifier: AssetLocalIdentifier?,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    ) {}

    override fun failedSharing(
        localIdentifier: AssetLocalIdentifier?,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    ) {}
}