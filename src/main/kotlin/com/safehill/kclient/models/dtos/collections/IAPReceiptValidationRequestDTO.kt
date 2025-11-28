package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IAPReceiptValidationRequestDTO(
    @SerialName("jws_transaction") val jwsTransaction: String,
    @SerialName("product_id") val productId: String,
    @SerialName("transaction_id") val transactionId: String
)
