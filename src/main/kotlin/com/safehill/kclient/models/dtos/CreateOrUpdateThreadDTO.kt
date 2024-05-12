package com.safehill.kclient.models.dtos

import com.safehill.kclient.network.dtos.RecipientEncryptionDetailsDTO
import kotlinx.serialization.Serializable

@Serializable
data class CreateOrUpdateThreadDTO(
    val name: String?,
    val recipients: List<RecipientEncryptionDetailsDTO>
)