package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SHSendCodeToUserRequestDTO(
    val countryCode: Int,
    val phoneNumber: Long,
    val code: String,
    val medium: Medium
) {
    enum class Medium {
        Phone, SMS;

        override fun toString(): String {
            return when (this) {
                Phone -> "phone"
                SMS -> "sms"
            }
        }
    }
}