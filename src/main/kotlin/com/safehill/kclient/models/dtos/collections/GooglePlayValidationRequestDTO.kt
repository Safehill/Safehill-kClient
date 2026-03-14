package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GooglePlayValidationRequestDTO(
    @SerialName("purchase_token") val purchaseToken: String,
    @SerialName("product_id") val productId: String
)
