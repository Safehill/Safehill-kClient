package com.safehill.kclient.models.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendCodeToUserRequestDTO(
    val countryCode: Int,
    val phoneNumber: Long,
    val code: String,
    val medium: Medium
) {
    enum class Medium {
        @SerialName("phone")
        Phone,

        @SerialName("sms")
        SMS
        ;
    }
}