package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SHGetUsersRequest(
    val userIdentifiers: List<String>
) {}