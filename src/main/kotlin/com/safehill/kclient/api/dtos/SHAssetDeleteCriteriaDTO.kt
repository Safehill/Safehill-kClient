package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SHAssetDeleteCriteriaDTO(
    val globalIdentifiers: List<String>
)