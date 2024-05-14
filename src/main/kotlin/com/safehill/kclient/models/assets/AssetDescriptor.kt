package com.safehill.kclient.models.assets

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.safehill.kclient.models.serde.AssetDescriptorSerializer
import com.safehill.kclient.models.users.UserIdentifier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

typealias GroupId = String

@Serializable(with = AssetDescriptorSerializer::class)
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

    @Serializable(with = AssetDescriptorSerializer.SharingInfoSerializer::class)
    interface SharingInfo {

        @Serializable(with = AssetDescriptorSerializer.SharingInfoSerializer.GroupInfoSerializer::class)
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

    class ListDeserializer : ResponseDeserializable<List<AssetDescriptor>> {
        override fun deserialize(content: String): List<AssetDescriptor> {
            return Json.decodeFromString(content)
        }
    }
}

