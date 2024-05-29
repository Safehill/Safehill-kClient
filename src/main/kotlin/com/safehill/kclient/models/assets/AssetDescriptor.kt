package com.safehill.kclient.models.assets

import com.safehill.kclient.models.users.UserIdentifier
import java.util.Date

typealias GroupId = String

interface AssetDescriptor : RemoteAssetIdentifiable {

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
    }

    interface SharingInfo {

        interface GroupInfo {
            /// The name of the asset group (optional)
            val name: String?
            /// ISO8601 formatted datetime, representing the time the asset group was created
            val createdAt: Date?
        }

        val sharedByUserIdentifier: UserIdentifier
        /// Maps user public identifiers to asset group identifiers
        val sharedWithUserIdentifiersInGroup: Map<UserIdentifier, GroupId>
        val groupInfoById: Map<GroupId, GroupInfo>

        fun userSharingInfo(userId: UserIdentifier): GroupInfo? {
            this.sharedWithUserIdentifiersInGroup[userId]?.let {
                return this.groupInfoById[it]
            }
            return null
        }
    }

    override val globalIdentifier: AssetGlobalIdentifier
    override val localIdentifier: AssetLocalIdentifier?
    val creationDate: Date?
    var uploadState: UploadState
    var sharingInfo: SharingInfo

}

