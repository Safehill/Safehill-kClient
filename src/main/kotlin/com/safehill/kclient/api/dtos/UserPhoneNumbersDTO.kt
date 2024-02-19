package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

typealias HashedPhoneNumber = String

@Serializable
data class UserPhoneNumbersDTO(
    val phoneNumbers: List<HashedPhoneNumber>
)