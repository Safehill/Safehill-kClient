package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.serde.ISO8601DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class AssetOutputDTO(
    val globalIdentifier: String,
    val localIdentifier: String?,
    @Serializable(with = ISO8601DateSerializer::class) val creationDate: Date?,
    val groupId: String,
    val versions: List<AssetVersionOutputDTO>,
)