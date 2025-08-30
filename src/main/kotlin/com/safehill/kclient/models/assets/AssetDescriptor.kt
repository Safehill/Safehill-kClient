package com.safehill.kclient.models.assets

import com.safehill.kclient.models.dtos.AssetPermission
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
    /// Maps user public identifiers to asset group identifiers
    val groupIdsByRecipientUserIdentifier: Map<UserIdentifier, List<GroupId>>,
    val groupInfoById: Map<GroupId, GroupInfo>
)

data class GroupInfo(
    /// The name of the asset group (optional)
    val name: String?,
    /// ISO8601 formatted datetime, representing the time the asset group was created
    val createdAt: Instant,
    /// Whether it's confidential, shareable or public. default to confidential
    val permissions: AssetPermission = AssetPermission.Confidential
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
