package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

typealias HashedPhoneNumber = String

@Serializable
data class UserPhoneNumbersDTO(
    val phoneNumbers: List<HashedPhoneNumber>
)