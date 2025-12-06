package com.safehill.kclient.models.assets

import com.safehill.kclient.models.dtos.AssetCollectionAccessType
import com.safehill.kclient.models.dtos.SharingOption
import com.safehill.kclient.models.dtos.collections.CollectionVisibility
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
    val groupInfoById: Map<GroupId, GroupInfo>,
    val collectionInfoById: Map<String, AssetCollectionInfo>
)

data class GroupInfo(
    val name: String?,
    val createdAt: Instant,
    val createdBy: UserIdentifier,
    val permissions: SharingOption,
    val createdFromThreadId: String?
)

data class AssetCollectionInfo(
    val collectionId: String,
    val collectionName: String,
    val visibility: CollectionVisibility,
    val accessType: AssetCollectionAccessType,
    val addedAt: Instant
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

    fun isCompleted() = this == Completed
}


val AssetDescriptor.sharedWithUserIdentifiers
    get() = this.sharingInfo.groupIdsByRecipientUserIdentifier.keys - this.createdByUserIdentifier