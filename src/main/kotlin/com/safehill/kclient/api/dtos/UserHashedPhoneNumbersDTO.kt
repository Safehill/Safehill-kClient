package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class UserHashedPhoneNumbersDTO(
    val phoneNumbers: List<String>
)