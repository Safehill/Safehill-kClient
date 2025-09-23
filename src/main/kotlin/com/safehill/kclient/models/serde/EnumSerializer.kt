package com.safehill.kclient.models.serde

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

abstract class EnumSerializer<T : Enum<T>, S> : KSerializer<T> {

    abstract fun serialize(item: T): S
    abstract fun deserialize(value: S): T
    abstract val serializationType: SerializationType

    override val descriptor: SerialDescriptor by lazy {
        PrimitiveSerialDescriptor("Enum", serializationType.primitiveKind)
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        return when (serializationType) {
            SerializationType.INT -> deserialize(decoder.decodeInt() as S)
            SerializationType.STRING -> deserialize(decoder.decodeString() as S)
            SerializationType.LONG -> deserialize(decoder.decodeLong() as S)
            SerializationType.DOUBLE -> deserialize(decoder.decodeDouble() as S)
            SerializationType.FLOAT -> deserialize(decoder.decodeFloat() as S)
            SerializationType.BOOLEAN -> deserialize(decoder.decodeBoolean() as S)
        }
    }

    override fun serialize(encoder: Encoder, value: T) {
        val serialized = serialize(value)
        when (serializationType) {
            SerializationType.INT -> encoder.encodeInt(serialized as Int)
            SerializationType.STRING -> encoder.encodeString(serialized as String)
            SerializationType.LONG -> encoder.encodeLong(serialized as Long)
            SerializationType.DOUBLE -> encoder.encodeDouble(serialized as Double)
            SerializationType.FLOAT -> encoder.encodeFloat(serialized as Float)
            SerializationType.BOOLEAN -> encoder.encodeBoolean(serialized as Boolean)
        }
    }
}