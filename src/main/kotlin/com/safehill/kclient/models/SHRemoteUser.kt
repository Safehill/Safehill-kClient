package com.safehill.kclient.models

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.safehill.kclient.api.serde.SHRemoteUserSerializer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.decodeStructure
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

    class MapInResultDeserializer : ResponseDeserializable<Map<String, SHRemoteUser>> {
        private val mapSerializer = MapSerializer(
            String.serializer(),
            SHRemoteUser.serializer()
        )

        override fun deserialize(content: String): Map<String, SHRemoteUser> {
            return Json.decodeFromString(
                object : DeserializationStrategy<Map<String, SHRemoteUser>> {
                    override val descriptor: SerialDescriptor =
                        buildClassSerialDescriptor("Map<String, SHRemoteUser>") {
                            element<Map<String, SHRemoteUser>>("result")
                        }

                    override fun deserialize(decoder: Decoder): Map<String, SHRemoteUser> {
                        return decoder.decodeStructure(descriptor) {
                            var map: Map<String, SHRemoteUser>? = null
                            when (val index = decodeElementIndex(descriptor)) {
                                0 -> map = decodeSerializableElement(
                                    descriptor = mapSerializer.descriptor,
                                    index = index,
                                    deserializer = mapSerializer
                                )
                            }
                            map!!
                        }
                    }
                },
                content,
            )
        }
    }

    class ListDeserializer : ResponseDeserializable<List<SHRemoteUser>> {
        override fun deserialize(content: String): List<SHRemoteUser> {
            return Json.decodeFromString(content)
        }
    }
}