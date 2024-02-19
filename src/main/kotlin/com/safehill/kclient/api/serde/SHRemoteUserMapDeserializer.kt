package com.safehill.kclient.api.serde

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.safehill.kclient.models.SHRemoteUser
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.json.Json

class SHRemoteUserMapDeserializer : ResponseDeserializable<Map<String, SHRemoteUser>> {
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
