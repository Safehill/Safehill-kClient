package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AuthChallengeRequestDTO(
    val identifier: String,
)
