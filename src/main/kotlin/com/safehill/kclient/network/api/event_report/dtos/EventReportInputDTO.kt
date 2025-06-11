package com.safehill.kclient.network.api.event_report.dtos

import kotlinx.serialization.Serializable

@Serializable
data class EventReportInputDTO(
    val type: Int,
    val scale: Int,
    val victimAskedHelpFrom: String,
    val victimTalkedAboutItWith: String,
    val victimThought: String,
    val otherDetails: String
)