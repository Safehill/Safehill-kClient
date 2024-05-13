package com.safehill.kclient.models.users

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.safehill.kclient.api.serde.RemoteUserSerializer
import com.safehill.kcrypto.models.SafehillPublicKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.PublicKey

@Serializable(with = RemoteUserSerializer::class)
data class RemoteUser(
    override val identifier: String,
    override val name: String,
    @SerialName("public_key")
    override val publicKeyData: ByteArray,
    @SerialName("public_signature")
    override val publicSignatureData: ByteArray
) : ServerUser {
    override val publicKey: PublicKey
        get() = SafehillPublicKey.from(publicKeyData)

    override val publicSignature: PublicKey
        get() = SafehillPublicKey.from(publicSignatureData)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteUser

        if (identifier != other.identifier) return false

        return true
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }

    class Deserializer : ResponseDeserializable<RemoteUser> {
        override fun deserialize(content: String): RemoteUser {
            return Json.decodeFromString(content)
        }
    }

}