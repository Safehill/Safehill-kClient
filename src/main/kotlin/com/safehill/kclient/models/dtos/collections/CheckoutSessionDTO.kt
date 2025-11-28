package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.Serializable

@Serializable
data class CheckoutSessionDTO(
    val sessionUrl: String,
    val sessionId: String,
    val amount: Double,
    val currency: String
)
