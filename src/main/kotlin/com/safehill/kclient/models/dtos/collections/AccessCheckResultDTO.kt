package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccessCheckResultDTO(
    val status: AccessStatus,
    val message: String? = null,
    val price: Double? = null,
    val visibility: CollectionVisibility? = null,
    val createdBy: String? = null
)


@Serializable
enum class AccessStatus {
    @SerialName("granted")
    GRANTED,

    @SerialName("paywall")
    PAYWALL,

    @SerialName("denied")
    DENIED,

    @SerialName("loading")
    LOADING
}