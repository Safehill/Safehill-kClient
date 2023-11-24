package com.safehill.kclient.models

data class SHSendCodeToUserRequestDTO(
    val countryCode: Int,
    val phoneNumber: Int,
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