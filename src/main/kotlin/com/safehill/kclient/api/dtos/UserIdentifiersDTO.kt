package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class UserIdentifiersDTO(
    val userIdentifiers: List<String>
) {}