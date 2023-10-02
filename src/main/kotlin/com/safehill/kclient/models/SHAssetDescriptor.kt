package com.safehill.kclient.models

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.safehill.kclient.api.serde.SHAssetDescriptorSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

@Serializable(with = SHAssetDescriptorSerializer::class)
interface SHAssetDescriptor : SHRemoteAssetIdentifiable {

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

    @Serializable(with = SHAssetDescriptorSerializer.SharingInfoSerializer::class)
    interface SharingInfo {

        @Serializable(with = SHAssetDescriptorSerializer.SharingInfoSerializer.GroupInfoSerializer::class)
        interface GroupInfo {
            /// The name of the asset group (optional)
            val name: String?
            /// ISO8601 formatted datetime, representing the time the asset group was created
            val createdAt: Date?
        }

        val sharedByUserIdentifier: String
        val sharedWithUserIdentifiersInGroup: Map<String, String>
        /// Maps user public identifiers to asset group identifiers
        val groupInfoById: Map<String, SHAssetDescriptor.SharingInfo.GroupInfo>

        fun userSharingInfo(userId: String): GroupInfo? {
            this.sharedWithUserIdentifiersInGroup[userId]?.let {
                return this.groupInfoById[it]
            }
            return null
        }
    }

    override val globalIdentifier: String
    override val localIdentifier: String?
    val creationDate: Date?
    var uploadState: UploadState
    var sharingInfo: SharingInfo

    class ListDeserializer : ResponseDeserializable<List<SHAssetDescriptor>> {
        override fun deserialize(content: String): List<SHAssetDescriptor> {
            return Json.decodeFromString(content)
        }
    }
}

