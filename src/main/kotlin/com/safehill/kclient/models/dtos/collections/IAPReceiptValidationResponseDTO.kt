package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IAPReceiptValidationResponseDTO(
    val success: Boolean,
    val message: String,
    @SerialName("transaction_id") val transactionId: String? = null
)
