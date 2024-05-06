package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class GetInteractionDTO(
    val per: Int,
    val page: Int,
    val referencedInteractionId: String?,
    val before: String?
)
