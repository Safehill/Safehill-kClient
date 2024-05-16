package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AssetDescriptorFilterCriteriaDTO(
    val globalIdentifiers: List<String>?,
    val groupIds: List<String>?,
    val after: String?
)
