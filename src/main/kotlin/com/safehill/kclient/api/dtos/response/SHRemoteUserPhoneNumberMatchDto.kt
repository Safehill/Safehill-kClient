package com.safehill.kclient.api.dtos.response

import com.safehill.kclient.models.SHRemoteUser
import kotlinx.serialization.Serializable

@Serializable
data class SHRemoteUserPhoneNumberMatchDto(
    val result: Map<String, SHRemoteUser>
)