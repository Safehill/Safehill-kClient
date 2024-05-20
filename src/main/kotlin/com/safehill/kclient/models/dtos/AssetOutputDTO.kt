package com.safehill.kclient.models.dtos

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.safehill.kclient.models.serde.ServerAssetSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Date

@Serializable(with = ServerAssetSerializer::class)
data class AssetOutputDTO(
    val globalIdentifier: String,
    val localIdentifier: String?,
    val creationDate: Date?,
    val groupId: String,
    val versions: List<AssetVersionOutputDTO>,
) {
    class Deserializer : ResponseDeserializable<AssetOutputDTO> {
        override fun deserialize(content: String): AssetOutputDTO {
            return Json.decodeFromString(content)
        }
    }

    class ListDeserializer : ResponseDeserializable<List<AssetOutputDTO>> {
        override fun deserialize(content: String): List<AssetOutputDTO> {
            return Json.decodeFromString(content)
        }
    }
}
