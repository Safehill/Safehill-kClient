package com.safehill.kclient.models

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.safehill.kclient.api.serde.SHRemoteUserSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable(with = SHRemoteUserSerializer::class)
data class SHRemoteUser(
    override val identifier: String,
    override val name: String,
    @SerialName("public_key")
    val publicKeyData: ByteArray,
    @SerialName("public_signature")
    val publicSignatureData: ByteArray
) : SHServerUser {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SHRemoteUser

        if (identifier != other.identifier) return false

        return true
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }

    class Deserializer : ResponseDeserializable<SHRemoteUser> {
        override fun deserialize(content: String): SHRemoteUser {
            return Json.decodeFromString(content)
        }
    }
}