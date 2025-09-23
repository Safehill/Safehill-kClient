package com.safehill.kclient.network.api.event_report.enum

import com.safehill.kclient.models.serde.EnumSerializer
import com.safehill.kclient.models.serde.SerializationType

enum class ViolenceType {
    Physical,
    Psychological,
    Economical,
    Sexual,
    Stalking,
    Other;

    fun toServerValue(): Int = serverValueMap[this]!!

    companion object {
        private val serverValueMap = mapOf(
            Physical to 1,
            Psychological to 2,
            Economical to 3,
            Sexual to 4,
            Stalking to 5,
            Other to 0
        )

        private val enumValueMap = serverValueMap.entries.associate { it.value to it.key }

        fun fromServerValue(value: Int): ViolenceType = enumValueMap[value] ?: Other
    }
}

object ViolenceTypeSerializer : EnumSerializer<ViolenceType, Int>() {
    override val serializationType = SerializationType.INT
    override fun serialize(item: ViolenceType): Int = item.toServerValue()
    override fun deserialize(value: Int): ViolenceType = ViolenceType.fromServerValue(value)
}
