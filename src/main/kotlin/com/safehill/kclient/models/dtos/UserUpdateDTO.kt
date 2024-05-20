package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class UserUpdateDTO(
    val identifier: String?,
    val name: String?,
    val email: String?,
    val phoneNumber: String?,
    val publicKey: String?,
    val publicSignature: String?
)
