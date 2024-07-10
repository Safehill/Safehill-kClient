package com.safehill.kclient.models.users

import com.safehill.kclient.models.serde.Base64DataSerializer
import com.safehill.kcrypto.models.SafehillPublicKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.PublicKey

@Serializable
data class RemoteUser(
    override val identifier: UserIdentifier,
    override val name: String,
    @SerialName("publicKey")
    @Serializable(with = Base64DataSerializer::class)
    override val publicKeyData: ByteArray,
    @SerialName("publicSignature")
    @Serializable(with = Base64DataSerializer::class)
    override val publicSignatureData: ByteArray,
    val phoneNumber: String? = null
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
        if (name != other.name) return false
        if (!publicKeyData.contentEquals(other.publicKeyData)) return false
        if (!publicSignatureData.contentEquals(other.publicSignatureData)) return false
        if (phoneNumber != other.phoneNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = identifier.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + publicKeyData.contentHashCode()
        result = 31 * result + publicSignatureData.contentHashCode()
        result = 31 * result + (phoneNumber?.hashCode() ?: 0)
        return result
    }


}
