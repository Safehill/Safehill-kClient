package com.safehill.kclient.api.serde

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetDescriptorImpl
import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import java.util.Date

object AssetDescriptorSerializer : KSerializer<AssetDescriptor> {
    object SharingInfoSerializer : KSerializer<AssetDescriptor.SharingInfo> {

        object GroupInfoSerializer : KSerializer<AssetDescriptor.SharingInfo.GroupInfo> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SHAssetDescriptor.SharingInfo.GroupInfo") {
                element<String>("name")
                element<String>("createdAt")
            }

            override fun serialize(encoder: Encoder, value: AssetDescriptor.SharingInfo.GroupInfo) {
                encoder.encodeStructure(descriptor) {
                    value.name?.let { encodeStringElement(descriptor, 0, it) }
                    value.createdAt?.let { encodeSerializableElement(SharingInfoSerializer.descriptor, 1, ISO8601DateSerializer, it) }
                }
            }

            override fun deserialize(decoder: Decoder): AssetDescriptor.SharingInfo.GroupInfo {
                return decoder.decodeStructure(descriptor) {
                    var name: String? = null
                    var createdAt: Date? = null

                    loop@ while (true) {
                        when (val index = decodeElementIndex(descriptor)) {
                            CompositeDecoder.DECODE_DONE -> break@loop

                            0 -> name = decodeStringElement(descriptor, 0)
                            1 -> createdAt = decodeSerializableElement(descriptor, 1, ISO8601DateSerializer)

                            else -> throw SerializationException("unexpected index $index")
                        }
                    }

                    AssetDescriptorImpl.SharingInfoImpl.GroupInfoImpl(
                        name = name,
                        createdAt = createdAt
                    )
                }
            }

        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SHAssetDescriptor.SharingInfo") {
            element<String>("sharedByUserIdentifier")
            element<Map<String, String>>("sharedWithUserIdentifiersInGroup")
            element<Map<String, AssetDescriptor.SharingInfo.GroupInfo>>("groupInfoById")
        }

        override fun serialize(encoder: Encoder, value: AssetDescriptor.SharingInfo) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.sharedByUserIdentifier)
                encodeSerializableElement(descriptor, 1, MapSerializer(String.serializer(), String.serializer()), value.sharedWithUserIdentifiersInGroup)
                encodeSerializableElement(descriptor, 2, MapSerializer(String.serializer(), GroupInfoSerializer), value.groupInfoById)
            }
        }

        override fun deserialize(decoder: Decoder): AssetDescriptor.SharingInfo {
            return decoder.decodeStructure(descriptor) {
                var sharedByUserIdentifier: String? = null
                var sharedWithUserIdentifiersInGroup: Map<String, String>? = null
                var groupInfoById: Map<String, AssetDescriptor.SharingInfo.GroupInfo>? = null

                loop@ while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        CompositeDecoder.DECODE_DONE -> break@loop

                        0 -> sharedByUserIdentifier = decodeStringElement(descriptor, 0)
                        1 -> sharedWithUserIdentifiersInGroup = decodeSerializableElement(descriptor, 1, MapSerializer(String.serializer(), String.serializer()))
                        2 -> groupInfoById = decodeSerializableElement(descriptor, 2, MapSerializer(String.serializer(), GroupInfoSerializer))

                        else -> throw SerializationException("unexpected index $index")
                    }
                }

                AssetDescriptorImpl.SharingInfoImpl(
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
        element<String>("creationDate")
        element<AssetDescriptor.UploadState>("uploadState")
        element<AssetDescriptor.SharingInfo>("sharingInfo")
    }

    override fun serialize(encoder: Encoder, value: AssetDescriptor) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.globalIdentifier)
            value.localIdentifier?.let { encodeStringElement(descriptor, 1, it) }
            value.creationDate?.let { encodeSerializableElement(descriptor, 2, ISO8601DateSerializer, it) }
            encodeStringElement(descriptor, 3, value.uploadState.toString())
            encodeSerializableElement(descriptor, 4, SharingInfoSerializer,
                value.sharingInfo
            )
        }
    }

    override fun deserialize(decoder: Decoder): AssetDescriptor {
        return decoder.decodeStructure(descriptor) {
            var globalIdentifier: String? = null
            var localIdentifier: String? = null
            var creationDate: Date? = null
            var uploadState: AssetDescriptor.UploadState? = null
            var sharingInfo: AssetDescriptor.SharingInfo? = null

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop

                    0 -> globalIdentifier = decodeStringElement(descriptor, 0)
                    1 -> localIdentifier = decodeStringElement(descriptor, 1)
                    2 -> creationDate = decodeSerializableElement(descriptor, 2, ISO8601DateSerializer)
                    3 -> uploadState = AssetDescriptor.UploadState.entries.firstOrNull { it.toString() == decodeStringElement(descriptor, 3) }
                    4 -> sharingInfo = decodeSerializableElement(descriptor, 4, SharingInfoSerializer)

                    else -> throw SerializationException("unexpected index $index")
                }
            }

            AssetDescriptorImpl(
                requireNotNull(globalIdentifier),
                localIdentifier,
                creationDate,
                requireNotNull(uploadState),
                requireNotNull(sharingInfo)
            )
        }
    }
}