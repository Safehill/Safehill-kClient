package com.safehill.kclient.api.dtos

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.safehill.kclient.api.serde.SHServerAssetSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Date

@Serializable(with = SHServerAssetSerializer::class)
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
}