package com.safehill.kclient.api.dtos

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import com.safehill.kclient.models.SHRemoteUser
import kotlinx.serialization.Serializable

typealias BearerToken = String

@Serializable
data class SHAuthResponseDTO(
    val user: SHRemoteUser,
    val bearerToken: BearerToken,
    val encryptionProtocolSalt: String,
    val metadata: MetaData
) {
    @Serializable
    data class MetaData(
        val isPhoneNumberVerified: Boolean,
        val forceReindex: Boolean
    )

    class Deserializer : ResponseDeserializable<SHAuthResponseDTO> {
        override fun deserialize(content: String): SHAuthResponseDTO =
            Gson().fromJson(content, SHAuthResponseDTO::class.java)
    }
}