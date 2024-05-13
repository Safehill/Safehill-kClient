package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.users.RemoteUser
import kotlinx.serialization.Serializable

@Serializable
data class RemoteUserPhoneNumberMatchDto(
    val result: Map<String, RemoteUser>
)