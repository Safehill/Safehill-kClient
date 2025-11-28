package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.Serializable

@Serializable
data class CollectionCreateDTO(
    val name: String,
    val description: String
)
