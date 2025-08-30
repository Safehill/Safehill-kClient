package com.safehill.kclient.models.assets

import com.safehill.kclient.models.dtos.SharingOption
import com.safehill.kclient.models.users.UserIdentifier
import java.time.Instant

typealias GroupId = String


data class AssetDescriptor(
    override val globalIdentifier: AssetGlobalIdentifier,
    override val localIdentifier: AssetLocalIdentifier,
    val creationDate: Instant,
    val uploadState: UploadState,
    val sharingInfo: SharingInfo
) : RemoteAssetIdentifiable {
    val createdByUserIdentifier: UserIdentifier = sharingInfo.sharedByUserIdentifier
}

data class SharingInfo(
    val sharedByUserIdentifier: UserIdentifier,
    val groupIdsByRecipientUserIdentifier: Map<UserIdentifier, List<GroupId>>,
    val groupInfoById: Map<GroupId, GroupInfo>
)

data class GroupInfo(
    val name: String?,
    val createdAt: Instant,
    val createdBy: UserIdentifier,
    val permissions: SharingOption
)

enum class UploadState {
    NotStarted, Partial, Completed, Failed;

    override fun toString(): String {
        return when (this) {
            NotStarted -> "not_started"
            Partial -> "partial"
            Completed -> "completed"
            Failed -> "failed"
        }
    }

    fun isDownloadable() = when (this) {
        Partial, Completed -> true
        NotStarted, Failed -> false
    }
}
