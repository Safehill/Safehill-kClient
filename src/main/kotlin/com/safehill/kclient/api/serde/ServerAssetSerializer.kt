package com.safehill.kclient.api.serde

import com.safehill.kclient.models.dtos.AssetOutputDTO
import com.safehill.kclient.models.dtos.AssetVersionOutputDTO
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import java.util.Date

object ServerAssetSerializer : KSerializer<com.safehill.kclient.models.dtos.AssetOutputDTO> {

    object ServerAssetVersionSerializer : KSerializer<AssetVersionOutputDTO> {
        override val descriptor: SerialDescriptor= buildClassSerialDescriptor("SHServerAsset") {
            element<String>("versionName")
            element<ByteArray>("ephemeralPublicKey")
            element<ByteArray>("publicSignature")
            element<ByteArray>("encryptedSecret")
            element<String?>("presignedURL")
            element<Int?>("presignedURLExpiresInMinutes")
        }

        override fun deserialize(decoder: Decoder): AssetVersionOutputDTO {
            return decoder.decodeStructure(descriptor) {
                var versionName: String? = null
                var ephemeralPublicKey: ByteArray? = null
                var publicSignature: ByteArray? = null
                var encryptedSecret: ByteArray? = null
                var presignedURL: String? = null
                var presignedURLExpiresInMinutes: Int? = null

                loop@ while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        CompositeDecoder.DECODE_DONE -> break@loop

                        0 -> versionName = decodeStringElement(descriptor, 0)
                        1 -> ephemeralPublicKey = decodeSerializableElement(descriptor, 1, Base64DataSerializer)
                        2 -> publicSignature = decodeSerializableElement(descriptor, 2, Base64DataSerializer)
                        3 -> encryptedSecret = decodeSerializableElement(descriptor, 3, Base64DataSerializer)
                        4 -> presignedURL = decodeStringElement(descriptor, 4)
                        5 -> presignedURLExpiresInMinutes = decodeIntElement(descriptor, 5)

                        else -> throw SerializationException("unexpected index $index")
                    }
                }

                AssetVersionOutputDTO(
                    requireNotNull(versionName),
                    publicKeyData = requireNotNull(ephemeralPublicKey),
                    publicSignatureData = requireNotNull(publicSignature),
                    requireNotNull(encryptedSecret),
                    presignedURL,
                    presignedURLExpiresInMinutes
                )
            }
        }

        override fun serialize(encoder: Encoder, value: AssetVersionOutputDTO) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.versionName)
                encodeSerializableElement(descriptor, 1, Base64DataSerializer, value.publicKeyData)
                encodeSerializableElement(descriptor, 2, Base64DataSerializer, value.publicSignatureData)
                encodeSerializableElement(descriptor, 3, Base64DataSerializer, value.encryptedSecret)
                value.presignedURL?.let { encodeStringElement(descriptor, 4, it) }
                value.presignedURLExpiresInMinutes?.let { encodeIntElement(descriptor, 5, it) }
            }
        }
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SHServerAsset") {
        element<String>("globalIdentifier")
        element<String>("localIdentifier")
        element<String>("creationDate")
        element<String>("groupId")
        element<List<AssetVersionOutputDTO>>("versions")
    }

    override fun serialize(encoder: Encoder, value: com.safehill.kclient.models.dtos.AssetOutputDTO) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.globalIdentifier)
            value.localIdentifier?.let { encodeStringElement(descriptor, 1, it) }
            value.creationDate?.let { encodeSerializableElement(descriptor, 2, ISO8601DateSerializer, it) }
            encodeStringElement(descriptor, 3, value.groupId)
            encodeSerializableElement(descriptor, 4, ListSerializer(ServerAssetVersionSerializer), value.versions)
        }
    }

    override fun deserialize(decoder: Decoder): com.safehill.kclient.models.dtos.AssetOutputDTO {
        return decoder.decodeStructure(descriptor) {
            var globalIdentifier: String? = null
            var localIdentifier: String? = null
            var creationDate: Date? = null
            var groupId: String? = null
            var versions: List<AssetVersionOutputDTO>? = null

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop

                    0 -> globalIdentifier = decodeStringElement(descriptor, 0)
                    1 -> localIdentifier = decodeStringElement(descriptor, 1)
                    2 -> creationDate = decodeSerializableElement(descriptor, 2, ISO8601DateSerializer)
                    3 -> groupId = decodeStringElement(descriptor, 3)
                    4 -> versions = decodeSerializableElement(descriptor, 4, ListSerializer(ServerAssetVersionSerializer))

                    else -> throw SerializationException("unexpected index $index")
                }
            }

            com.safehill.kclient.models.dtos.AssetOutputDTO(
                requireNotNull(globalIdentifier),
                localIdentifier,
                creationDate,
                requireNotNull(groupId),
                requireNotNull(versions)
            )
        }
    }
}
