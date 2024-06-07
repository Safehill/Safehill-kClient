package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AssetSearchCriteriaDTO(
    val globalIdentifiers: List<String>,
    val versionNames: List<String>?
)
