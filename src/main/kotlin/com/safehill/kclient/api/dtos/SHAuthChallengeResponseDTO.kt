package com.safehill.kclient.api.dtos

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import kotlinx.serialization.Serializable

@Serializable
data class SHAuthChallengeResponseDTO(
    val challenge: String,
    val ephemeralPublicKey: String, // base64EncodedData
    val ephemeralPublicSignature: String, // base64EncodedData
    val publicKey: String, // base64EncodedData
    val publicSignature: String, // base64EncodedData
    val protocolSalt: String, // base64EncodedData
    val iv: String? // base64EncodedData
) {
    class Deserializer : ResponseDeserializable<SHAuthChallengeResponseDTO> {
        override fun deserialize(content: String): SHAuthChallengeResponseDTO
                = Gson().fromJson(content, SHAuthChallengeResponseDTO::class.java)
    }
}