package com.safehill.kclient.models.serde

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

abstract class EnumIntSerializer<T : Enum<T>> : KSerializer<T> {

    abstract fun codeSelector(item: T): Int
    abstract fun fromCode(int: Int): T

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EnumInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): T {
        return fromCode(decoder.decodeInt())
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeInt(
            codeSelector(value)
        )
    }
}