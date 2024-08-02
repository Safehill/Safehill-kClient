package com.safehill.kclient.models.dtos

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import kotlinx.serialization.Serializable

@Serializable
data class AuthChallengeResponseDTO(
    val challenge: String,
    val ephemeralPublicKey: String, // base64EncodedData
    val ephemeralPublicSignature: String, // base64EncodedData
    val publicKey: String, // base64EncodedData
    val publicSignature: String, // base64EncodedData
    val protocolSalt: String, // base64EncodedData
    val iv: String? // base64EncodedData
) {
    class Deserializer : ResponseDeserializable<AuthChallengeResponseDTO> {
        override fun deserialize(content: String): AuthChallengeResponseDTO
                = Gson().fromJson(content, AuthChallengeResponseDTO::class.java)
    }
}
