package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class UserIdentifiersDTO(
    val userIdentifiers: List<String>
) {}