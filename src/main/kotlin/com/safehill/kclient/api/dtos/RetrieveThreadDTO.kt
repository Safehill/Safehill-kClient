package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class RetrieveThreadDTO(
    val byUsersPublicIdentifiers: List<String>
)