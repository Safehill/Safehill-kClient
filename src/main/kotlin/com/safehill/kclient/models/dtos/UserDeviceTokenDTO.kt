package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

const val FCM_TOKEN_TYPE = 1

@Serializable
data class UserDeviceTokenDTO(
    val deviceId: String,
    val token: String,
    val tokenType: Int
)