package com.safehill.kclient.network.api.event_report

import com.safehill.kclient.network.api.event_report.enum.HelpSeekingSource
import com.safehill.kclient.network.api.event_report.enum.ViolenceSolution
import com.safehill.kclient.network.api.event_report.enum.ViolenceType

typealias EventIdentifier = String

interface EventReportApi {

    suspend fun postEventReport(
        violenceType: ViolenceType,
        severity: Int,
        victimAskedHelpFrom: HelpSeekingSource,
        victimTalkedAboutItWith: HelpSeekingSource,
        victimThought: ViolenceSolution,
        otherDetails: String
    ): EventIdentifier

}