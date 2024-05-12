package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class UserSearchDTO(
    val query: String,
    val per: Int,
    val page: Int
) {}