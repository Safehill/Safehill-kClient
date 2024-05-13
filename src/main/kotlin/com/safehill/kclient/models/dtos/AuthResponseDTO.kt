package com.safehill.kclient.models.dtos
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
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

    class Deserializer : ResponseDeserializable<AuthResponseDTO> {
        override fun deserialize(content: String): AuthResponseDTO =
            Gson().fromJson(content, AuthResponseDTO::class.java)
    }
}