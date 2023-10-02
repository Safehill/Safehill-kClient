package com.safehill.kclient.api.serde

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.util.Date


private val BASE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
private val FORMATTER = DateTimeFormatterBuilder().appendPattern(BASE_PATTERN).toFormatter()

object ISO8601DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ISO8601Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(value.toIso8601String())
    }

    override fun deserialize(decoder: Decoder): Date {
        val stringDate = decoder.decodeString()
        return stringDate.toIso8601Date()
    }
}

fun Date.toIso8601String(): String {
    val localDate = LocalDateTime.ofInstant(this.toInstant(), ZoneOffset.UTC)
    val offsetDate = OffsetDateTime.of(localDate, ZoneOffset.UTC)
//    return offsetDate.format(DateTimeFormatter.ISO_DATE_TIME)
    return offsetDate.format(FORMATTER)
}

fun String.toIso8601Date(): Date {
    val localDate = OffsetDateTime.parse(this).toLocalDateTime()
    val instant = localDate.toInstant(ZoneOffset.UTC)
    return Date.from(instant)
}