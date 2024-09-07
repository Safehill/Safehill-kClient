package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrUpdateThreadDTO(
    val name: String?,
    val recipients: List<RecipientEncryptionDetailsDTO>,
    val phoneNumbers: List<HashedPhoneNumber>
)
