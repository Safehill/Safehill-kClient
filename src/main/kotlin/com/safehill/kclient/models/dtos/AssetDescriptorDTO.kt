package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetDescriptorImpl
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.UploadState
import com.safehill.kclient.models.serde.InstantSerializer
import com.safehill.kclient.models.users.UserIdentifier
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AssetDescriptorDTO(
    @Serializable(with = InstantSerializer::class) val creationDate: Instant,
    val globalIdentifier: String,
    val localIdentifier: String,
    val sharingInfo: SharingInfoDTO,
    val uploadState: String
)

@Serializable
data class SharingInfoDTO(
    val groupInfoById: Map<GroupId, GroupInfoDTO>,
    val sharedByUserIdentifier: String,
    val groupIdsByRecipientUserIdentifier: Map<UserIdentifier, List<GroupId>>
)

@Serializable
data class GroupInfoDTO(
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val name: String?
)

fun AssetDescriptorDTO.toAssetDescriptor(): AssetDescriptor {
    return AssetDescriptorImpl(
        globalIdentifier = globalIdentifier,
        localIdentifier = localIdentifier,
        creationDate = creationDate,
        uploadState = UploadState.entries.first { it.toString() == uploadState },
        sharingInfo = AssetDescriptorImpl.SharingInfoImpl(
            sharedByUserIdentifier = sharingInfo.sharedByUserIdentifier,
            groupIdsByRecipientUserIdentifier = sharingInfo.groupIdsByRecipientUserIdentifier,
            groupInfoById = sharingInfo.groupInfoById.mapValues {
                with(it.value) {
                    AssetDescriptorImpl.SharingInfoImpl.GroupInfoImpl(
                        createdAt = this.createdAt,
                        name = this.name
                    )
                }
            }
        )
    )
}