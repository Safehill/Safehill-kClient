package com.safehill.kclient.network.api.event_report

import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.RequestMethod
import com.safehill.kclient.network.api.event_report.dtos.EventReportDTO
import com.safehill.kclient.network.api.event_report.dtos.EventReportInputDTO
import com.safehill.kclient.network.api.event_report.enum.HelpSeekingSource
import com.safehill.kclient.network.api.event_report.enum.ViolenceSolution
import com.safehill.kclient.network.api.event_report.enum.ViolenceType
import com.safehill.kclient.network.api.fireRequest
import com.safehill.kclient.network.api.postRequestForResponse

class EventReportApiImpl(
    private val baseApi: BaseApi
) : EventReportApi, BaseApi by baseApi {

    override suspend fun postEventReport(
        violenceType: ViolenceType,
        severity: Int,
        victimAskedHelpFrom: HelpSeekingSource,
        victimTalkedAboutItWith: HelpSeekingSource,
        victimThought: ViolenceSolution,
        otherDetails: String
    ): EventIdentifier {
        return baseApi.postRequestForResponse(
            endPoint = "/nova-stream-event",
            request = EventReportInputDTO(
                type = violenceType,
                scale = severity,
                victimAskedHelpFrom = victimAskedHelpFrom,
                victimTalkedAboutItWith = victimTalkedAboutItWith,
                victimThought = victimThought,
                otherDetails = otherDetails
            )
        )
    }

    override suspend fun getEventReports(): List<EventReportDTO> {
        return baseApi.fireRequest<Unit, List<EventReportDTO>>(
            requestMethod = RequestMethod.Get(emptyList()),
            endPoint = "/nova-stream-event"
        )
    }

}