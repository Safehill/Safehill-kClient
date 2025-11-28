package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CheckoutSessionUIMode {
    @SerialName("hosted")
    HOSTED,
    @SerialName("embedded")
    EMBEDDED
}

@Serializable
data class CreateCheckoutSessionRequestDTO(
    @SerialName("ui_mode") val uiMode: CheckoutSessionUIMode,
    @SerialName("web_base_url") val webBaseUrl: String? = null
)
