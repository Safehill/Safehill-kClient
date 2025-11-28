package com.safehill.kclient.models.dtos.collections

import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.serde.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class CollectionOutputDTO(
    val id: String,
    val name: String,
    val description: String,
    val isSystemCollection: Boolean,
    val isArchived: Boolean,
    val assetCount: Int,
    val visibility: CollectionVisibility,
    val pricing: Double,
    @Serializable(with = InstantSerializer::class) val lastUpdated: Instant,
    val createdBy: String,
    val assets: List<AssetOutputDTO>
)

/**
 * Collection visibility levels
 */
@Serializable
enum class CollectionVisibility {
    @SerialName("public")
    PUBLIC,

    @SerialName("confidential")
    CONFIDENTIAL,

    @SerialName("not-shared")
    NOT_SHARED
}
