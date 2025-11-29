package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CollectionSearchDTO(
    val query: String? = null,
    val searchScope: SearchScope, // "owned" for user's owned and accessed, "all" for all discoverable collections
    val visibility: CollectionVisibility? = null, // Optional filter by visibility
    val priceRange: PriceRangeDTO? = null
)

enum class SearchScope {
    @SerialName("owned")
    Owned,

    @SerialName("all")
    All
}

@Serializable
data class PriceRangeDTO(
    val min: Double? = null,
    val max: Double? = null
)
