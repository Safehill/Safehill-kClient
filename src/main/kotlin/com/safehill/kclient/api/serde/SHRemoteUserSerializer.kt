package com.safehill.kclient.api.serde

import com.safehill.kclient.models.users.RemoteUser
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*

object SHRemoteUserSerializer : KSerializer<RemoteUser> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SHRemoteUser") {
        element<String>("identifier")
        element<String>("name")
        element<ByteArray>("publicKey")
        element<ByteArray>("publicSignature")
    }

    override fun serialize(encoder: Encoder, value: RemoteUser) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.identifier)
            encodeStringElement(descriptor, 1, value.name)
            encodeSerializableElement(descriptor, 2, Base64DataSerializer, value.publicKeyData)
            encodeSerializableElement(descriptor, 3, Base64DataSerializer, value.publicSignatureData)
        }
    }

    override fun deserialize(decoder: Decoder): RemoteUser {
        return decoder.decodeStructure(descriptor) {
            var identifier: String? = null
            var name: String? = null
            var publicKeyData: ByteArray? = null
            var publicSignatureData: ByteArray? = null

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop

                    0 -> identifier = decodeStringElement(descriptor, 0)
                    1 -> name = decodeStringElement(descriptor, 1)
                    2 -> publicKeyData = decodeSerializableElement(descriptor, 2, Base64DataSerializer)
                    3 -> publicSignatureData = decodeSerializableElement(descriptor, 3, Base64DataSerializer)

                    else -> throw SerializationException("unexpected index $index")
                }
            }

            RemoteUser(
                requireNotNull(identifier),
                requireNotNull(name),
                requireNotNull(publicKeyData),
                requireNotNull(publicSignatureData)
            )
        }
    }
}