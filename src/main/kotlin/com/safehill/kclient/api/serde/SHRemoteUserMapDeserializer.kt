package com.safehill.kclient.api.serde

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.safehill.kclient.api.dtos.HashedPhoneNumber
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

class SHRemoteUserMapDeserializer : ResponseDeserializable<Map<HashedPhoneNumber, SHRemoteUser>> {
    private val mapSerializer = MapSerializer(
        HashedPhoneNumber.serializer(),
        SHRemoteUser.serializer()
    )

    override fun deserialize(content: String): Map<HashedPhoneNumber, SHRemoteUser> {
        return Json.decodeFromString(
            object : DeserializationStrategy<Map<HashedPhoneNumber, SHRemoteUser>> {
                override val descriptor: SerialDescriptor =
                    buildClassSerialDescriptor("Map<HashedPhoneNumber, SHRemoteUser>") {
                        element<Map<HashedPhoneNumber, SHRemoteUser>>("result")
                    }

                override fun deserialize(decoder: Decoder): Map<HashedPhoneNumber, SHRemoteUser> {
                    return decoder.decodeStructure(descriptor) {
                        var map: Map<HashedPhoneNumber, SHRemoteUser>? = null
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
