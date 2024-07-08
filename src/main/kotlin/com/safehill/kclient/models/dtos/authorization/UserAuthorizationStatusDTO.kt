package com.safehill.kclient.models.dtos.authorization

import com.safehill.kclient.models.users.RemoteUser
import kotlinx.serialization.Serializable

@Serializable
data class UserAuthorizationStatusDTO(
    val pending: List<RemoteUser>,
    val blocked: List<RemoteUser>
)