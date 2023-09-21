package com.safehill.kclient.api.serde

import com.safehill.kclient.models.SHAssetDescriptor
import com.safehill.kclient.models.SHAssetDescriptorImpl
import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import java.util.Date

object SHAssetDescriptorSerializer : KSerializer<SHAssetDescriptor> {
    object SharingInfoSerializer : KSerializer<SHAssetDescriptor.SharingInfo> {

        object GroupInfoSerializer : KSerializer<SHAssetDescriptor.SharingInfo.GroupInfo> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SHAssetDescriptor.SharingInfo.GroupInfo") {
                element<String>("name")
                element<Date>("createdAt")
            }

            override fun deserialize(decoder: Decoder): SHAssetDescriptor.SharingInfo.GroupInfo {
                TODO("Not yet implemented")
            }

            override fun serialize(encoder: Encoder, value: SHAssetDescriptor.SharingInfo.GroupInfo) {
                TODO("Not yet implemented")
            }

        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SHAssetDescriptor.SharingInfo") {
            element<String>("sharedByUserIdentifier")
            element<Map<String, String>>("sharedWithUserIdentifiersInGroup")
            element<Map<String, SHAssetDescriptor.SharingInfo.GroupInfo>>("groupInfoById")
        }

        override fun serialize(encoder: Encoder, value: SHAssetDescriptor.SharingInfo) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.sharedByUserIdentifier)
                encodeSerializableElement(descriptor, 1, MapSerializer(String.serializer(), String.serializer()), value.sharedWithUserIdentifiersInGroup)
                encodeSerializableElement(descriptor, 2, MapSerializer(String.serializer(), GroupInfoSerializer), value.groupInfoById)
            }
        }

        override fun deserialize(decoder: Decoder): SHAssetDescriptor.SharingInfo {
            return decoder.decodeStructure(descriptor) {
                var sharedByUserIdentifier: String? = null
                var sharedWithUserIdentifiersInGroup: Map<String, String>? = null
                var groupInfoById: Map<String, SHAssetDescriptor.SharingInfo.GroupInfo>? = null

                loop@ while (true) {
                    when (val index = decodeElementIndex(SHAssetDescriptorSerializer.descriptor)) {
                        CompositeDecoder.DECODE_DONE -> break@loop

                        0 -> sharedByUserIdentifier = decodeStringElement(descriptor, 0)
                        1 -> sharedWithUserIdentifiersInGroup = decodeSerializableElement(descriptor, 1, MapSerializer(String.serializer(), String.serializer()))
                        2 -> groupInfoById = decodeSerializableElement(descriptor, 2, MapSerializer(String.serializer(), GroupInfoSerializer))

                        else -> throw SerializationException("unexpected index $index")
                    }
                }

                SHAssetDescriptorImpl.SharingInfoImpl(
                    requireNotNull(sharedByUserIdentifier),
                    requireNotNull(sharedWithUserIdentifiersInGroup),
                    requireNotNull(groupInfoById)
                )
            }
        }

    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SHAssetDescriptor") {
        element<String>("globalIdentifier")
        element<String>("localIdentifier")
        element<Date>("creationDate")
        element<SHAssetDescriptor.UploadState>("uploadState")
        element<SHAssetDescriptor.SharingInfo>("sharingInfo")
    }

    override fun serialize(encoder: Encoder, value: SHAssetDescriptor) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.globalIdentifier)
            value.localIdentifier?.let { encodeStringElement(descriptor, 1, it) }
            value.creationDate?.let { encodeSerializableElement(descriptor, 2, ISO8601DateSerializer, it) }
            encodeStringElement(descriptor, 3, value.uploadState.toString())
            encodeSerializableElement(descriptor, 4, SharingInfoSerializer,
                value.sharingInfo as SHAssetDescriptor.SharingInfo
            )
        }
    }

    override fun deserialize(decoder: Decoder): SHAssetDescriptor {
        return decoder.decodeStructure(descriptor) {
            var globalIdentifier: String? = null
            var localIdentifier: String? = null
            var creationDate: Date? = null
            var uploadState: SHAssetDescriptor.UploadState? = null
            var sharingInfo: SHAssetDescriptor.SharingInfo? = null

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop

                    0 -> globalIdentifier = decodeStringElement(descriptor, 0)
                    1 -> localIdentifier = decodeStringElement(descriptor, 1)
                    2 -> creationDate = decodeSerializableElement(descriptor, 2, ISO8601DateSerializer)
                    3 -> uploadState = SHAssetDescriptor.UploadState.valueOf(decodeStringElement(descriptor, 3))
                    4 -> sharingInfo = decodeSerializableElement(descriptor, 4, SharingInfoSerializer)

                    else -> throw SerializationException("unexpected index $index")
                }
            }

            SHAssetDescriptorImpl(
                requireNotNull(globalIdentifier),
                localIdentifier,
                creationDate,
                requireNotNull(uploadState),
                requireNotNull(sharingInfo)
            )
        }
    }
}