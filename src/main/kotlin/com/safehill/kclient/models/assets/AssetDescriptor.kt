package com.safehill.kclient.models.assets

import com.safehill.kclient.models.users.UserIdentifier
import java.time.Instant
import java.util.Date

typealias GroupId = String

interface AssetDescriptor : RemoteAssetIdentifiable {
    override val globalIdentifier: AssetGlobalIdentifier
    override val localIdentifier: AssetLocalIdentifier
    val creationDate: Instant
    var uploadState: UploadState
    var sharingInfo: SharingInfo
}


interface SharingInfo {
    val sharedByUserIdentifier: UserIdentifier

    /// Maps user public identifiers to asset group identifiers
    val groupIdsByRecipientUserIdentifier: Map<UserIdentifier, List<GroupId>>
    val groupInfoById: Map<GroupId, GroupInfo>
}

interface GroupInfo {
    /// The name of the asset group (optional)
    val name: String?

    /// ISO8601 formatted datetime, representing the time the asset group was created
    val createdAt: Date?
}

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
