package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SHUserIdentifiersDTO(
    val userIdentifiers: List<String>
) {}