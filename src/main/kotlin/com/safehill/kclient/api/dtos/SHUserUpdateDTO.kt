package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SHUserUpdateDTO(
    val identifier: String?,
    val name: String?,
    val email: String?,
    val phoneNumber: String?,
    val publicKey: String?,
    val publicSignature: String?
) {}