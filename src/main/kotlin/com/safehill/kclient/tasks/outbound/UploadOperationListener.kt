package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.ServerUser

interface UploadOperationListener {

    fun enqueued(
        threadId: String,
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: String
    ) {
    }

    fun startedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
    ) {
    }

    fun finishedEncrypting(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
    ) {
    }

    fun failedEncrypting(
        globalIdentifier: AssetGlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
    ) {
    }

    fun startedUploading(
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
    ) {
    }

    fun finishedUploading(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
    ) {
    }

    fun failedUploading(
        globalIdentifier: AssetGlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
    ) {
    }

    fun startedSharing(
        localIdentifier: AssetLocalIdentifier?,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    ) {
    }

    fun finishedSharing(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    ) {
    }

    fun failedSharing(
        localIdentifier: AssetLocalIdentifier?,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    ) {
    }
}

interface UploadOperationErrorListener : UploadOperationListener {

    fun onError(
        globalIdentifier: AssetGlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        uploadFailure: UploadFailure
    )

    override fun failedEncrypting(
        globalIdentifier: AssetGlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
    ) {
        onError(
            localIdentifier = localIdentifier,
            groupId = groupId,
            uploadFailure = UploadFailure.ENCRYPTION,
            globalIdentifier = globalIdentifier
        )
    }

    override fun failedUploading(
        globalIdentifier: AssetGlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
    ) {
        onError(
            localIdentifier = localIdentifier,
            groupId = groupId,
            uploadFailure = UploadFailure.UPLOAD,
            globalIdentifier = globalIdentifier
        )
    }

    override fun failedSharing(
        localIdentifier: AssetLocalIdentifier?,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    ) {
        if (localIdentifier != null) {
            onError(
                localIdentifier = localIdentifier,
                groupId = groupId,
                uploadFailure = UploadFailure.SHARING,
                globalIdentifier = globalIdentifier
            )
        }
    }
}