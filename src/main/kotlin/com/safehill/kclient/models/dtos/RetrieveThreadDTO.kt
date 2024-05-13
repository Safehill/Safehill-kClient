package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class RetrieveThreadDTO(
    val byUsersPublicIdentifiers: List<String>
)