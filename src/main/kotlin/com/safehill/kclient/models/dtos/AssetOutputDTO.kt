package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.serde.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AssetOutputDTO(
    val globalIdentifier: String,
    val localIdentifier: String,
    @Serializable(with = InstantSerializer::class) val creationDate: Instant,
    val versions: List<AssetVersionOutputDTO>,
)