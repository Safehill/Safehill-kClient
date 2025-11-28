package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.Serializable

@Serializable
data class CollectionUpdateDTO(
    val name: String? = null,
    val description: String? = null,
    val pricing: Double? = null
)
