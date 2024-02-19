package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class UserPhoneNumbersDTO(
    val phoneNumbers: List<String>
)