package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class PaginationMetadataDTO(
    val page: Int,
    val per: Int,
    val total: Int
)