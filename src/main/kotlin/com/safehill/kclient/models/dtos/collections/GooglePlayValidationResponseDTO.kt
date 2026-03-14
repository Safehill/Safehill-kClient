package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GooglePlayValidationResponseDTO(
    val success: Boolean,
    val message: String,
    @SerialName("purchase_token") val purchaseToken: String? = null
)
