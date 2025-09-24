package com.safehill.kclient.network.api.event_report.dtos

import com.safehill.kclient.models.serde.InstantSerializer
import com.safehill.kclient.network.api.event_report.enum.HelpSeekingSource
import com.safehill.kclient.network.api.event_report.enum.HelpSeekingSourceSerializer
import com.safehill.kclient.network.api.event_report.enum.ViolenceSolution
import com.safehill.kclient.network.api.event_report.enum.ViolenceSolutionSerializer
import com.safehill.kclient.network.api.event_report.enum.ViolenceType
import com.safehill.kclient.network.api.event_report.enum.ViolenceTypeSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class EventReportDTO(
    val identifier: String,
    @Serializable(with = ViolenceTypeSerializer::class)
    val type: ViolenceType,
    val scale: Int,
    @Serializable(with = HelpSeekingSourceSerializer::class)
    val victimAskedHelpFrom: HelpSeekingSource,
    @Serializable(with = HelpSeekingSourceSerializer::class)
    val victimTalkedAboutItWith: HelpSeekingSource,
    @Serializable(with = ViolenceSolutionSerializer::class)
    val victimThought: ViolenceSolution,
    val otherDetails: String,
    @Serializable(with = InstantSerializer::class)
    val timeCreated: Instant
)
