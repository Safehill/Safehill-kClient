package com.safehill.kclient.api.dtos

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class SHSendCodeToUserRequestDTO(
    val countryCode: Int,
    val phoneNumber: Long,
    val code: String,
    val medium: Medium
) {
    enum class Medium {
        @SerializedName("phone")
        Phone,
        @SerializedName("sms")
        SMS
        ;
    }
}