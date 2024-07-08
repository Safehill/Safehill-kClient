package com.safehill.kclient.models.dtos.authorization

import kotlinx.serialization.Serializable

@Serializable
data class UserAuthorizationRequestDTO(
    val userPublicIdentifiers: List<String>
)
