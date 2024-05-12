package com.safehill.kclient.api.dtos

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.safehill.kclient.api.serde.SHServerAssetSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable(with = SHServerAssetSerializer.SHServerAssetVersionSerializer::class)
data class AssetVersionOutputDTO(
    val versionName: String,
    @SerialName("ephemeralPublicKey")
    val publicKeyData: ByteArray,
    @SerialName("publicSignature")
    val publicSignatureData: ByteArray,
    val encryptedSecret: ByteArray,
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


    class Deserializer : ResponseDeserializable<AssetVersionOutputDTO> {
        override fun deserialize(content: String): AssetVersionOutputDTO {
            return Json.decodeFromString(content)
        }
    }
}
