package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AssetDeleteCriteriaDTO(
    val globalIdentifiers: List<String>
)
