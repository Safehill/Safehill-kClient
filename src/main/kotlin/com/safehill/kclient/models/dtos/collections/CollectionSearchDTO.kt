package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.Serializable

@Serializable
data class CollectionSearchDTO(
    val query: String? = null,
    val searchScope: String, // "owned" for user's owned and accessed, "all" for all discoverable collections
    val visibility: CollectionVisibility? = null, // Optional filter by visibility
    val priceRange: PriceRangeDTO? = null
)

@Serializable
data class PriceRangeDTO(
    val min: Double? = null,
    val max: Double? = null
)
