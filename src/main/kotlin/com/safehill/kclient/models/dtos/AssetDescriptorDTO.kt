package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetDescriptorImpl
import com.safehill.kclient.models.serde.toIso8601Date
import kotlinx.serialization.Serializable

@Serializable
data class AssetDescriptorDTO(
    val creationDate: String,
    val globalIdentifier: String,
    val localIdentifier: String,
    val sharingInfo: SharingInfo,
    val uploadState: String
)

@Serializable
data class SharingInfo(
    val groupInfoById: Map<String, GroupInfo>,
    val sharedByUserIdentifier: String,
    val sharedWithUserIdentifiersInGroup: Map<String, String>
)

@Serializable
data class GroupInfo(
    val createdAt: String,
    val name: String?
)

fun AssetDescriptorDTO.toAssetDescriptor(): AssetDescriptor {
    return AssetDescriptorImpl(
        globalIdentifier = globalIdentifier,
        localIdentifier = localIdentifier,
        creationDate = creationDate.toIso8601Date(),
        uploadState = AssetDescriptor.UploadState.entries.first { it.toString() == uploadState },
        sharingInfo = AssetDescriptorImpl.SharingInfoImpl(
            sharedByUserIdentifier = sharingInfo.sharedByUserIdentifier,
            sharedWithUserIdentifiersInGroup = sharingInfo.sharedWithUserIdentifiersInGroup,
            groupInfoById = sharingInfo.groupInfoById.mapValues {
                with(it.value) {
                    AssetDescriptorImpl.SharingInfoImpl.GroupInfoImpl(
                        createdAt = this.createdAt.toIso8601Date(),
                        name = this.name
                    )
                }
            }
        )
    )
}