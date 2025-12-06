package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.assets.AssetCollectionInfo
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.GroupInfo
import com.safehill.kclient.models.assets.SharingInfo
import com.safehill.kclient.models.assets.UploadState
import com.safehill.kclient.models.dtos.collections.CollectionVisibility
import com.safehill.kclient.models.serde.InstantSerializer
import com.safehill.kclient.models.users.UserIdentifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AssetDescriptorDTO(
    @Serializable(with = InstantSerializer::class) val creationDate: Instant,
    val globalIdentifier: String,
    val localIdentifier: String?,
    val sharingInfo: SharingInfoDTO,
    val uploadState: String
)

@Serializable
data class SharingInfoDTO(
    val groupInfoById: Map<GroupId, GroupInfoDTO>,
    val sharedByUserIdentifier: String,
    val groupIdsByRecipientUserIdentifier: Map<UserIdentifier, List<GroupId>>,
    val collectionInfoById: Map<String, AssetCollectionInfoDTO>
)

@Serializable
data class GroupInfoDTO(
    /// ISO8601 formatted datetime, representing the time the asset group was created
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    /// The name of the asset group (optional)
    val name: String?,
    /// The identifier of the user that created the group (introduced in Nov 2024)
    val createdBy: UserIdentifier,
    /// Whether it's confidential, shareable or public. null will default to confidential
    val permissions: SharingOption?,
    /// Whether or not the share group was created from a thread (namely is a photo message)
    val createdFromThreadId: String?
)


@Serializable
data class AssetCollectionInfoDTO(
    val collectionId: String,
    val collectionName: String,
    val visibility: CollectionVisibility,
    val accessType: AssetCollectionAccessType,
    /// ISO8601 formatted datetime, representing when the asset was added to this collection
    @Serializable(with = InstantSerializer::class)
    val addedAt: Instant
)

@Serializable
enum class AssetCollectionAccessType {
    @SerialName("granted")
    GRANTED,

    @SerialName("accessed")
    ACCESSED,

    @SerialName("payment")
    PAYMENT
}

fun AssetDescriptorDTO.toAssetDescriptor(): AssetDescriptor {
    return AssetDescriptor(
        globalIdentifier = globalIdentifier,
        // The current implementation depend upon localIdentifier not being null.
        // Setting localIdentifier to empty works, but a refactor is needed to handle this cleanly.
        localIdentifier = localIdentifier.orEmpty(),
        creationDate = creationDate,
        uploadState = UploadState.entries.first { it.toString() == uploadState },
        sharingInfo = SharingInfo(
            sharedByUserIdentifier = sharingInfo.sharedByUserIdentifier,
            groupIdsByRecipientUserIdentifier = sharingInfo.groupIdsByRecipientUserIdentifier,
            groupInfoById = sharingInfo.groupInfoById.mapValues {
                with(it.value) {
                    GroupInfo(
                        createdAt = this.createdAt,
                        name = this.name,
                        createdBy = this.createdBy,
                        permissions = this.permissions ?: SharingOption.Confidential,
                        createdFromThreadId = this.createdFromThreadId
                    )
                }
            },
            collectionInfoById = sharingInfo.collectionInfoById.mapValues {
                with(it.value) {
                    AssetCollectionInfo(
                        collectionId = this.collectionId,
                        collectionName = this.collectionName,
                        visibility = this.visibility,
                        accessType = this.accessType,
                        addedAt = this.addedAt
                    )
                }
            }
        )
    )
}