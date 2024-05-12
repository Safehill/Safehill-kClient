package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class UserSearchDTO(
    val query: String,
    val per: Int,
    val page: Int
) {}