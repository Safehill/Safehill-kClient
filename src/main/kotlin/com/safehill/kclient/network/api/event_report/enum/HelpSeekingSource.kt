package com.safehill.kclient.network.api.event_report.enum

import com.safehill.kclient.models.serde.EnumSerializer
import com.safehill.kclient.models.serde.SerializationType

enum class HelpSeekingSource {
    Police,
    AntiViolenceCenter,
    SocialServices,
    HealthServices,
    FamilyRelativesFriends,
    Pharmacy,
    Nobody,
    Other;

    fun toServerValue(): String = serverValueMap[this]!!

    companion object {
        private val serverValueMap = mapOf(
            Police to "Police",
            AntiViolenceCenter to "Anti Violence Center",
            SocialServices to "Social Service",
            HealthServices to "Health Services",
            FamilyRelativesFriends to "Family, Relatives and Friends",
            Pharmacy to "Pharmacy",
            Nobody to "Nobody",
            Other to "Other"
        )

        private val enumValueMap = serverValueMap.entries.associate { it.value to it.key }

        fun fromServerValue(value: String): HelpSeekingSource = enumValueMap[value] ?: Other
    }
}

object HelpSeekingSourceSerializer : EnumSerializer<HelpSeekingSource, String>() {
    override val serializationType = SerializationType.STRING
    override fun serialize(item: HelpSeekingSource): String = item.toServerValue()
    override fun deserialize(value: String): HelpSeekingSource = HelpSeekingSource.fromServerValue(value)
}