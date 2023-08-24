package com.safehill.kclient.api.dtos

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import kotlinx.serialization.Serializable

@Serializable
data class SHAuthChallenge(
    val challenge: String,
    val ephemeralPublicKey: String, // base64EncodedData
    val ephemeralPublicSignature: String, // base64EncodedData
    val publicKey: String, // base64EncodedData
    val publicSignature: String // base64EncodedData
) {
    class Deserializer : ResponseDeserializable<SHAuthChallenge> {
        override fun deserialize(content: String): SHAuthChallenge
                = Gson().fromJson(content, SHAuthChallenge::class.java)
    }
}