package com.safehill.kclient.network.api.event_report.enum

import com.safehill.kclient.models.serde.EnumSerializer
import com.safehill.kclient.models.serde.SerializationType

enum class ViolenceSolution {
    MoveOut,
    Divorce,
    DenounceToAuthorities,
    Nothing;

    fun toServerValue(): String = serverValueMap[this]!!

    companion object {
        private val serverValueMap = mapOf(
            MoveOut to "Move Out",
            Divorce to "Divorce",
            DenounceToAuthorities to "Denounce to Authorities",
            Nothing to "Nothing"
        )

        private val enumValueMap = serverValueMap.entries.associate { it.value to it.key }

        fun fromServerValue(value: String): ViolenceSolution = enumValueMap[value] ?: Nothing
    }
}

object ViolenceSolutionSerializer : EnumSerializer<ViolenceSolution, String>() {
    override val serializationType = SerializationType.STRING
    override fun serialize(item: ViolenceSolution): String = item.toServerValue()
    override fun deserialize(value: String): ViolenceSolution = ViolenceSolution.fromServerValue(value)
}
