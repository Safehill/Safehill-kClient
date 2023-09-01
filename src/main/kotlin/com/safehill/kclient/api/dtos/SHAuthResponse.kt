package com.safehill.kclient.api.dtos

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import com.safehill.kclient.models.SHRemoteUser
import kotlinx.serialization.Serializable

typealias BearerToken = String

@Serializable
data class SHAuthResponse(
    val user: SHRemoteUser,
    val bearerToken: BearerToken,
    val encryptionProtocolSalt: String
) {
    class Deserializer : ResponseDeserializable<SHAuthResponse> {
        override fun deserialize(content: String): SHAuthResponse
                = Gson().fromJson(content, SHAuthResponse::class.java)
    }
}