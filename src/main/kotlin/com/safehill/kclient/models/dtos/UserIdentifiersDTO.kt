package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.users.UserIdentifier
import kotlinx.serialization.Serializable

@Serializable
data class UserIdentifiersDTO(
    val userIdentifiers: List<UserIdentifier>
)
