package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.serde.Base64DataSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssetVersionOutputDTO(
    val versionName: String,
    @SerialName("ephemeralPublicKey")
    @Serializable(with = Base64DataSerializer::class) val publicKeyData: ByteArray,
    @SerialName("publicSignature")
    @Serializable(with = Base64DataSerializer::class) val publicSignatureData: ByteArray,
    @Serializable(with = Base64DataSerializer::class) val encryptedSecret: ByteArray,
    val presignedURL: String? = null,
    val presignedURLExpiresInMinutes: Int? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AssetVersionOutputDTO

        if (versionName != other.versionName) return false
        if (!publicKeyData.contentEquals(other.publicKeyData)) return false
        if (!publicSignatureData.contentEquals(other.publicSignatureData)) return false
        if (!encryptedSecret.contentEquals(other.encryptedSecret)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = versionName.hashCode()
        result = 31 * result + publicKeyData.contentHashCode()
        result = 31 * result + publicSignatureData.contentHashCode()
        result = 31 * result + encryptedSecret.contentHashCode()
        return result
    }

}
