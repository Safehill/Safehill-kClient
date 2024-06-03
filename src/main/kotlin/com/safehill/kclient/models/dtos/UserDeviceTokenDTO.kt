package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class UserDeviceTokenDTO(
    val deviceId: String,
    val token: String,
    val tokenType: Int
)