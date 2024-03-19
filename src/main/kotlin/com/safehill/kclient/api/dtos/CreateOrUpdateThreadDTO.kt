package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrUpdateThreadDTO(
    val name: String?,
    val recipients: List<SHRecipientEncryptionDetailsDTO>
)