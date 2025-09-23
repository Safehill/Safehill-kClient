package com.safehill.kclient.models.serde

import kotlinx.serialization.descriptors.PrimitiveKind

enum class SerializationType(val primitiveKind: PrimitiveKind) {
    INT(PrimitiveKind.INT),
    STRING(PrimitiveKind.STRING),
    LONG(PrimitiveKind.LONG),
    DOUBLE(PrimitiveKind.DOUBLE),
    FLOAT(PrimitiveKind.FLOAT),
    BOOLEAN(PrimitiveKind.BOOLEAN)
}