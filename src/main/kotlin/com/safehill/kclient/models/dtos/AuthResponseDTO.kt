package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.users.RemoteUser
import kotlinx.serialization.Serializable

typealias BearerToken = String

@Serializable
data class AuthResponseDTO(
    val user: RemoteUser,
    val bearerToken: BearerToken,
    val encryptionProtocolSalt: String,
    val metadata: MetaData
) {
    @Serializable
    data class MetaData(
        val isPhoneNumberVerified: Boolean,
        val forceReindex: Boolean
    )
}
