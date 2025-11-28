package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.dtos.collections.PublicAssetVersionOutputDTO
import com.safehill.kclient.models.serde.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AssetOutputDTO(
    val globalIdentifier: String,
    val localIdentifier: String? = null,
    val createdBy: String,
    @Serializable(with = InstantSerializer::class) val creationDate: Instant? = null,
    val uploadState: String,
    val versions: List<AssetVersionOutputDTO>,
    /// Indicates if this asset is publicly accessible without encryption
    /// Optional for backward compatibility with existing clients
    val isPublic: Boolean? = null,
    /// Public versions with direct access URLs (only present when isPublic = true)
    val publicVersions: List<PublicAssetVersionOutputDTO>? = null
)