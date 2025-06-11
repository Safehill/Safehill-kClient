package com.safehill.kclient.network.api.event_report

import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.event_report.dtos.EventReportInputDTO
import com.safehill.kclient.network.api.event_report.enum.HelpSeekingSource
import com.safehill.kclient.network.api.event_report.enum.ViolenceSolutions
import com.safehill.kclient.network.api.event_report.enum.ViolenceType
import com.safehill.kclient.network.api.postRequestForResponse

class EventReportApiImpl(
    private val baseApi: BaseApi
) : EventReportApi, BaseApi by baseApi {

    override suspend fun postEventReport(
        violenceType: ViolenceType,
        severity: Int,
        victimAskedHelpFrom: HelpSeekingSource,
        victimTalkedAboutItWith: HelpSeekingSource,
        victimThought: ViolenceSolutions,
        otherDetails: String
    ): EventIdentifier {
        return baseApi.postRequestForResponse(
            endPoint = "/nova-stream-event",
            request = EventReportInputDTO(
                type = violenceType.toServerValue(),
                scale = severity,
                victimAskedHelpFrom = victimAskedHelpFrom.toServerValue(),
                victimTalkedAboutItWith = victimTalkedAboutItWith.toServerValue(),
                victimThought = victimThought.toServerValue(),
                otherDetails = otherDetails
            )
        )
    }

    private fun ViolenceType.toServerValue(): Int = when (this) {
        ViolenceType.Physical -> 0
        ViolenceType.Psychological -> 1
    }

    private fun HelpSeekingSource.toServerValue(): String = when (this) {
        HelpSeekingSource.Police -> "Police"
        HelpSeekingSource.AntiViolenceCenter -> "Anti Violence Center"
        HelpSeekingSource.SocialServices -> "Social Service"
        HelpSeekingSource.HealthServices -> "Health Services"
        HelpSeekingSource.FamilyRelativesFriends -> "Family, Relatives and Friends"
        HelpSeekingSource.Nobody -> "Nobody"
        HelpSeekingSource.Other -> "Other"
    }

    private fun ViolenceSolutions.toServerValue(): String = when (this) {
        ViolenceSolutions.MoveOut -> "Move Out"
        ViolenceSolutions.Divorce -> "Divorce"
        ViolenceSolutions.DenounceToAuthorities -> "Denounce to Authorities"
        ViolenceSolutions.Nothing -> "Nothing"
    }
}