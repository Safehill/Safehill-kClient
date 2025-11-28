package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.Serializable

@Serializable
data class AccessCheckResultDTO(
    val status: String, // 'granted' | 'paywall' | 'denied' | 'loading'
    val message: String? = null,
    val price: Double? = null,
    val visibility: CollectionVisibility? = null,
    val createdBy: String? = null
)
